export const meta = {
  name: 'kn-ic-fuzz',
  description: 'Fuzz Kotlin/Native incremental compilation with LLM-written multi-module edit scenarios; oracle: IC build == clean build',
  whenToUse: 'Hunt for K/N IC bugs (stale caches, IC-only link failures or crashes, wrong diagnostics). Args: {runId, rounds, perRound, mutations, variants, focus, targets, dist, smoke}',
  phases: [
    { title: 'Prepare', detail: 'check the dist, warm up the stdlib caches' },
    { title: 'Distill', detail: 'read the IC code and its bug history, list attack hypotheses' },
    { title: 'Plan', detail: 'pick diverse targets for the round, steered by coverage and findings' },
    { title: 'Fuzz', detail: 'write scenarios, run the harness, repair invalid ones, mutate valid ones' },
    { title: 'Verify', detail: 'try to refute each finding, then reduce the confirmed ones' },
    { title: 'Report', detail: 'group by root cause, promote reproducers to regression tests' },
  ],
}

// ---------------------------------------------------------------------------------------------------------------------
// Arguments

const A = args || {}
const pick = (value, fallback) => (value === undefined || value === null ? fallback : value)
const RUN = String(pick(A.runId, 'run')).replace(/[^A-Za-z0-9._-]/g, '-')
const SMOKE = Boolean(A.smoke)
// The defaults keep a run to about 10 agents; pass e.g. {rounds: 3, perRound: 8} for a real campaign.
const ROUNDS = SMOKE ? 1 : pick(A.rounds, 1)
const PER_ROUND = SMOKE ? 1 : pick(A.perRound, 4)
const MUTATIONS = SMOKE ? 0 : pick(A.mutations, 2)
const VARIANTS = pick(A.variants, SMOKE ? ['default'] : ['default', 'unbatched'])
const FOCUS = pick(A.focus, '')
const DIST = pick(A.dist, '')

const WORK = `build/kn-ic-fuzz/${RUN}`
const H = 'python3 .claude/kn-ic-fuzz/harness.py' + (DIST ? ` --dist ${DIST}` : '')
const VFLAGS = VARIANTS.map(v => `--variant ${v}`).join(' ')
const CORPUS = 'native/native.tests/testData/caches/ic'
const IC_SRC = 'kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan'

const CONTEXT = `You are one agent in a fuzzing campaign against Kotlin/Native incremental compilation (IC) in this repository.
First read .claude/kn-ic-fuzz/README.md: it explains the oracle (IC build must behave like a clean build), the scenario
format (the one of ${CORPUS}) and the harness. Rules:
- Run commands from the repository root. Write files only under ${WORK}/. Never edit compiler sources or ${CORPUS}.
- Run a scenario with: ${H} run <scenario dir> --out <new out dir> ${VFLAGS}
  It takes 20-120s: give Bash a 600000 ms timeout. Run one harness process at a time.
- The harness prints a short summary. Trust it. Compiler logs (<out>/steps/<n>/*.log) are long: read them only with
  grep/head/sed for the part you need, e.g. grep -A40 "IC analysis" <out>/steps/2/ic-default.log.
- If the JetBrains MCP server is not reachable, use the standard file tools for files under ${WORK}/.`

// ---------------------------------------------------------------------------------------------------------------------
// Schemas

const str = { type: 'string' }
const strList = { type: 'array', items: str }
const VERDICT = { type: 'string', enum: ['PASS', 'BUG', 'LATENT', 'INVALID', 'PERF'] }

const PREP_SCHEMA = {
  type: 'object',
  properties: { ok: { type: 'boolean' }, warmup: str, distBuilt: str, headDate: str, stale: { type: 'boolean' }, note: str },
  required: ['ok', 'warmup', 'stale'],
}

const HYPOTHESES_SCHEMA = {
  type: 'object',
  properties: {
    hypotheses: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          title: str,
          mechanism: { type: 'string', description: 'what the IC code does or misses, with file:line' },
          edit: { type: 'string', description: 'a concrete edit sequence that would expose it' },
          topology: { type: 'string', description: 'modules and files involved, e.g. lib1 <- lib2 <- main, friend, external' },
          observable: { type: 'string', description: 'what main must print so the stale code shows up' },
        },
        required: ['title', 'mechanism', 'edit'],
      },
    },
  },
  required: ['hypotheses'],
}

const PLAN_SCHEMA = {
  type: 'object',
  properties: {
    targets: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          slug: { type: 'string', description: 'short kebab-case name' },
          title: str,
          strategy: { type: 'string', enum: ['fresh', 'mutate-corpus', 'combine'] },
          seed: { type: 'string', description: `for mutate-corpus/combine: case dirs relative to ${CORPUS}` },
          plan: { type: 'string', description: 'modules, files, and the edit of every step' },
          observable: str,
        },
        required: ['slug', 'title', 'strategy', 'plan'],
      },
    },
  },
  required: ['targets'],
}

const FINDING = {
  type: 'object',
  properties: { kind: str, step: str, variant: str, detail: str },
  required: ['kind', 'detail'],
}

const GEN_SCHEMA = {
  type: 'object',
  properties: {
    attempts: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          scenarioDir: str,
          outDir: str,
          harnessFlags: { type: 'string', description: 'extra flags used, e.g. --roundtrip' },
          verdict: VERDICT,
          findings: { type: 'array', items: FINDING },
          coverage: strList,
          change: { type: 'string', description: 'what this attempt changed compared with the previous one' },
        },
        required: ['scenarioDir', 'outDir', 'verdict'],
      },
    },
    verdict: VERDICT,
    bestScenarioDir: { type: 'string', description: 'the attempt with the most severe verdict' },
    bestOutDir: str,
    bestHarnessFlags: str,
    note: str,
  },
  required: ['attempts', 'verdict'],
}

const VERIFY_SCHEMA = {
  type: 'object',
  properties: {
    confirmed: { type: 'boolean' },
    kind: str,
    step: str,
    variant: str,
    icSpecific: { type: 'boolean', description: 'false if the clean build is wrong too' },
    signature: { type: 'string', description: 'short root-cause label, e.g. inline-body-of-transitive-dependency-not-tracked' },
    mechanism: { type: 'string', description: 'which file should have been rebuilt and why it was not' },
    knownIssue: { type: 'string', description: 'KT id or corpus case of the same bug, if any' },
    reason: str,
  },
  required: ['confirmed', 'reason'],
}

const REDUCE_SCHEMA = {
  type: 'object',
  properties: {
    reducedDir: str,
    outDir: str,
    steps: { type: 'number' },
    files: { type: 'number' },
    stillReproduces: { type: 'boolean' },
    summary: str,
  },
  required: ['reducedDir', 'stillReproduces', 'summary'],
}

const REPORT_SCHEMA = {
  type: 'object',
  properties: {
    markdown: str,
    bugs: {
      type: 'array',
      items: {
        type: 'object',
        properties: { title: str, kind: str, signature: str, promotedDir: str, promotedReproduces: { type: 'boolean' } },
        required: ['title', 'kind'],
      },
    },
  },
  required: ['markdown', 'bugs'],
}

// ---------------------------------------------------------------------------------------------------------------------
// Helpers

const ORDER = ['BUG', 'LATENT', 'INVALID', 'PERF', 'PASS']
const worst = verdicts => ORDER.find(v => verdicts.includes(v)) || 'PASS'
const slugify = s => String(s || 'target').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 32)
const compact = o => JSON.stringify(o, null, 1)

// ---------------------------------------------------------------------------------------------------------------------
// Prepare

phase('Prepare')
const prep = await agent(`${CONTEXT}

Prepare the fuzzing run ${RUN}.
1. Run: ${H} warmup   (it builds the stdlib caches of the link flavor; up to 2 minutes on a cold dist).
2. Compare the dist build time (stat -f %Sm kotlin-native/dist/konan/lib/kotlin-native-compiler-embeddable.jar, or the
   --dist one) with the date of the latest commit that touched ${IC_SRC} (git log -1 --format=%ci -- ${IC_SRC}).
   stale = the dist is older than that commit. Do not rebuild the dist.
3. mkdir -p ${WORK}
Report ok=false only if the warmup fails.`, { label: 'prepare', phase: 'Prepare', schema: PREP_SCHEMA, effort: 'low' })

if (!prep || !prep.ok) {
  log(`Warmup failed: ${prep ? prep.warmup : 'agent died'}. Stopping.`)
  return { ok: false, prep }
}
if (prep.stale) log(`Warning: the dist (${prep.distBuilt}) is older than the latest IC change (${prep.headDate}). Findings may be already fixed.`)

// ---------------------------------------------------------------------------------------------------------------------
// Distill: WhiteFox-style source reading and Mut4All/IssueMut-style bug-history mining

let hypotheses = []
if (!SMOKE && !A.targets) {
  phase('Distill')
  const DISTILLERS = [
    {
      key: 'code',
      prompt: `Read the Kotlin/Native IC implementation and find where it could miss an invalidation.
Start with ${IC_SRC}/CacheBuilder.kt (dirty-file analysis), CachedLibraries.kt, CacheSupport.kt, and find with grep:
the per-file dependency tracker (DependenciesTracker, CertainFiles / WholeModule / weak dependencies), the IR file
fingerprint (SerializedIrFileFingerprint), and the writers of the per-file cache parts ir/class_fields, ir/eager_init,
ir/inline_bodies, ir/trivial_getters, ir/objc_adapters. For each way a file's generated code can depend on another file
(inlining, vtable/itable layout, fake overrides, field layout, eager init, constants, default arguments, enum entries,
sealed subclasses, type checks, companion/object init, typealiases, suspend lowering, ...), decide whether a dependency
is recorded and whether a change of the other file changes its fingerprint. Each hypothesis is one place where the
answer might be "no". Cite file:line. Give 8-12 hypotheses, the most likely first.`,
    },
    {
      key: 'history',
      prompt: `Mine the history of Kotlin/Native IC bugs and turn it into edit classes, the way Mut4All and IssueMut turn bug reports
into mutators. Look at: git log --format='%h %ad %s' --date=short -- ${IC_SRC}/CacheBuilder.kt ${IC_SRC}/CachedLibraries.kt
and the history of ${CORPUS} (git log --stat -- ${CORPUS}). For the fix commits, read the message and the reproducer test
that came with it. List the corpus case names (ls ${CORPUS}/*/). Then find gaps: combinations of edit kind x module
topology (same lib, cross-lib, transitive, friend, external, cinterop) x sequence shape (revert, add-then-remove,
rename-back, move-back, flag toggle, 4-6 steps) that no corpus case covers, near the places where bugs were found before.
Give 8-12 hypotheses, the most likely first.`,
    },
  ]
  const distilled = await parallel(DISTILLERS.map(d => () =>
    agent(`${CONTEXT}\n\n${d.prompt}${FOCUS ? `\n\nFocus on: ${FOCUS}` : ''}`,
      { label: `distill:${d.key}`, phase: 'Distill', schema: HYPOTHESES_SCHEMA })))
  hypotheses = distilled.filter(Boolean).flatMap(r => r.hypotheses)
  log(`${hypotheses.length} hypotheses from ${distilled.filter(Boolean).length} distillers`)
}

// ---------------------------------------------------------------------------------------------------------------------
// Rounds: plan -> fuzz -> verify -> reduce

const SMOKE_TARGET = {
  slug: 'smoke-inline-transitive',
  title: 'Inline function body change seen through a non-inline caller in another library',
  strategy: 'fresh',
  plan: 'Modules lib1 <- lib2 <- main. lib1 has `inline fun value(): Int`. lib2 has `fun viaLib2() = value() * 10` and an inline ' +
    '`inline fun viaLib2Inline() = value() + 1`. main prints viaLib2(), viaLib2Inline() and value(). Steps: 0 initial; 1 change the ' +
    'body of value() only (lib2 source unchanged); 2 revert to step 0 body.',
  observable: 'every value, one per line',
}

const history = []
const confirmed = []
const perfNotes = []
const seenFeatures = new Set()
let dryRounds = 0

for (let round = 1; round <= ROUNDS; round++) {
  // -- Plan
  let targets
  if (SMOKE) {
    targets = [SMOKE_TARGET]
  } else if (A.targets && round === 1) {
    targets = A.targets
  } else {
    const plan = await agent(`${CONTEXT}

You plan round ${round} of ${ROUNDS} of the fuzzing campaign. Pick ${PER_ROUND} targets. Each target becomes one generator
agent that writes a scenario, runs it, and mutates it ${MUTATIONS} times.

Attack hypotheses (from reading the IC code and its bug history):
${compact(hypotheses)}

Results of earlier rounds (target, strategy, verdict, IC coverage features, findings):
${history.length ? compact(history) : 'none yet'}

IC coverage features seen so far: ${[...seenFeatures].join(', ') || 'none'}
${FOCUS ? `Focus of this campaign: ${FOCUS}` : ''}

Rules:
- Make the targets differ from each other and from earlier ones: different hypotheses, edit kinds, topologies, sequence shapes.
- About half "fresh" (new scenario from a hypothesis), the rest "mutate-corpus" (copy a corpus case from ${CORPUS} and extend
  it: longer or reverted sequences, another topology, a sibling language feature) or "combine" (merge two corpus cases into
  one project so that their edits interact). Plain mutation of an existing test suite is a strong baseline; use it.
- Steer by feedback: prefer hypotheses next to findings of earlier rounds (variations of a confirmed bug often find its
  siblings), and features that were never seen, e.g. dirty-propagated-cross-library, removed-file, full-rebuild-of-cached.
- Drop hypotheses that earlier rounds tested with PASS several times.
- The plan must be concrete: modules with dependencies, files, the edit of every step, and what main prints.`,
      { label: `plan:r${round}`, phase: 'Plan', schema: PLAN_SCHEMA })
    targets = plan ? plan.targets : hypotheses.slice((round - 1) * PER_ROUND, round * PER_ROUND).map(h => ({
      slug: h.title, title: h.title, strategy: 'fresh', plan: `${h.edit}\nTopology: ${h.topology || '-'}`, observable: h.observable,
    }))
  }
  if (targets.length > PER_ROUND) {
    log(`Round ${round}: the planner proposed ${targets.length} targets, running the first ${PER_ROUND}`)
    targets = targets.slice(0, PER_ROUND)
  }
  targets = targets.map((t, i) => ({ ...t, id: `r${round}t${i + 1}-${slugify(t.slug || t.title)}` }))
  log(`Round ${round}: ${targets.length} targets`)

  const results = await pipeline(
    targets,

    // -- Fuzz: generate, run, repair (ReFuzzer-style), mutate (Fuzz4All-style)
    t => {
      const dir = `${WORK}/${t.id}`
      const seed = t.seed ? `\nSeed corpus case(s): ${t.seed} (under ${CORPUS}). Copy with cp -R into ${dir}/s0 first.` : ''
      return agent(`${CONTEXT}

Target: ${t.title}
Strategy: ${t.strategy}${seed}
Plan: ${t.plan}
What main should print: ${t.observable || 'every value the edits can change'}

Write the first scenario to ${dir}/s0 and run it with --out ${dir}/s0-out. Keep scenarios small: 2-4 modules, 2-6 files,
3-6 steps. The main module uses fun main() and prints values (convert corpus @Test mains to prints).
Then loop:
1. INVALID: read the reason, fix the scenario in place, rerun into a new out dir. At most 3 repairs, then stop.
2. PASS: make up to ${MUTATIONS} mutated scenarios ${dir}/s1, ${dir}/s2, ... and run each. Before each mutation, look at
   what the IC did (the summary's dirty= counts; DIRTY FILES in the IC log). A file whose generated code depends on the
   edit but is not dirty is the signal: push there. Mutation kinds: extend the sequence (revert, add-then-remove,
   rename-back, move-back); change the topology (same lib, cross-lib, transitive, friend, external); swap the edited feature
   for a sibling one; or rerun with --roundtrip (no new files needed).
3. BUG or LATENT: stop mutating. Rerun that scenario once into a new out dir to see that it reproduces. Do not reduce it.
PERF findings alone do not stop the loop.
Report every harness run as an attempt; step values as strings.`,
        { label: `gen:${t.id}`, phase: 'Fuzz', schema: GEN_SCHEMA })
    },

    // -- Verify: an independent agent tries to refute the finding
    (gen, t) => {
      if (!gen) return { gen: null }
      const verdict = worst((gen.attempts || []).map(a => a.verdict).concat([gen.verdict]))
      if (verdict !== 'BUG' && verdict !== 'LATENT') return { gen, verdict }
      const best = (gen.attempts || []).find(a => a.verdict === verdict) || {}
      const scenarioDir = best.scenarioDir || gen.bestScenarioDir
      const flags = best.harnessFlags || gen.bestHarnessFlags || ''
      return agent(`${CONTEXT}

A fuzzing run reported a possible IC bug. Try to REFUTE it. Report confirmed=false unless it reproduces cleanly and is a
real IC bug.
Scenario: ${scenarioDir} (harness flags used: ${flags || 'none'})
First run: ${best.outDir || gen.bestOutDir}
Reported findings: ${compact(best.findings || [])}

1. Rerun from scratch: ${H} run ${scenarioDir} --out ${WORK}/${t.id}/verify-out --self-check ${VFLAGS} ${flags}
   If the finding is gone, or HARNESS_NOISE shows up, it is not confirmed.
2. Read the scenario. The output must be deterministic and must not depend on unspecified behaviour. If it does,
   it is not confirmed.
3. For OUTPUT_MISMATCH, diff steps/<n>/clean.stdout with steps/<n>/ic-<variant>.stdout and say which value is stale.
   For link failures and crashes, quote the first error or the top frames.
4. Read the IC analysis of that step (grep -A40 "IC analysis" <out>/steps/<n>/ic-<variant>.log). Name the file that should
   have been dirty and was not, and the dependency that IC missed. Check the code in ${IC_SRC} if needed.
5. Look for the same known bug: similar corpus cases, and git log --oneline -i --grep=<keyword> -- ${IC_SRC}.
   A known bug is still confirmed=true if it reproduces: put the KT id or case into knownIssue.${DIST ? `
   This campaign runs against another compiler (${DIST}) on purpose: a bug already fixed in this repository's HEAD still counts.` : ''}`,
        { label: `verify:${t.id}`, phase: 'Verify', schema: VERIFY_SCHEMA })
        .then(v => ({ gen, verdict, verify: v, scenarioDir, flags }))
    },

    // -- Reduce: deterministic shrinking first (cheap), then LLM-guided semantic shrinking (LPR-style)
    (prev, t) => {
      if (!prev || !prev.verify || !prev.verify.confirmed) return prev
      const rdir = `${WORK}/${t.id}/reduce`
      const variant = prev.verify.variant && VARIANTS.includes(prev.verify.variant) ? `--variant ${prev.verify.variant}` : VFLAGS
      return agent(`${CONTEXT}

Reduce this confirmed IC bug: ${prev.verify.kind} at step ${prev.verify.step} (${prev.verify.variant}).
Mechanism: ${prev.verify.mechanism || '-'}
Scenario: ${prev.scenarioDir} (harness flags: ${prev.flags || 'none'})

1. Run the deterministic reducer. It may take up to 15 minutes: run it with run_in_background and wait for the
   notification. ${H} reduce ${prev.scenarioDir} --out ${rdir} --max-runs 12 --kind ${prev.verify.kind} ${variant} ${prev.flags}
   The result is ${rdir}/reduced (if it says "nothing to reduce", start from the original scenario).
2. Then shrink by hand, the way ReduKtor and LPR do: copy the result to ${rdir}/manual and remove declarations,
   statements, prints, supertypes, files and modules that do not matter; simplify names. After each change run
   ${H} run ${rdir}/manual --out ${rdir}/manual-out-<k> --no-noop ${variant}
   and keep the change only if the same finding kind shows up at the same step. At most 8 runs.
3. Check that the final scenario reproduces. Explain the mechanism in 3-5 sentences.`,
        { label: `reduce:${t.id}`, phase: 'Verify', schema: REDUCE_SCHEMA })
        .then(r => ({ ...prev, reduce: r }))
    },
  )

  // -- Bookkeeping and the dry-round check (cross-target, so after the pipeline)
  let newFeatures = 0
  let newBugs = 0
  results.forEach((r, i) => {
    const t = targets[i]
    if (!r || !r.gen) {
      history.push({ round, target: t.title, strategy: t.strategy, verdict: 'AGENT_FAILED' })
      return
    }
    const attempts = r.gen.attempts || []
    attempts.forEach(a => (a.coverage || []).forEach(f => {
      if (!seenFeatures.has(f)) { seenFeatures.add(f); newFeatures++ }
    }))
    attempts.forEach(a => (a.findings || []).filter(f => /NOOP_REBUILD|UNSTABLE_FINGERPRINT/.test(f.kind))
      .forEach(f => perfNotes.push({ target: t.id, scenarioDir: a.scenarioDir, ...f })))
    const verdict = worst(attempts.map(a => a.verdict).concat([r.gen.verdict]))
    history.push({
      round,
      target: t.title,
      strategy: t.strategy,
      verdict,
      confirmed: r.verify ? r.verify.confirmed : undefined,
      signature: r.verify ? r.verify.signature : undefined,
      coverage: [...new Set(attempts.flatMap(a => a.coverage || []))],
      note: r.verify ? r.verify.reason : r.gen.note,
    })
    if (r.verify && r.verify.confirmed) {
      newBugs++
      confirmed.push({ target: t, verify: r.verify, reduce: r.reduce || null, scenarioDir: r.scenarioDir, flags: r.flags })
    }
  })
  log(`Round ${round}: ${newBugs} confirmed, ${newFeatures} new coverage features, ${confirmed.length} confirmed in total`)
  dryRounds = newBugs === 0 && newFeatures === 0 ? dryRounds + 1 : 0
  if (dryRounds >= 2 && round < ROUNDS) {
    log(`Two dry rounds in a row: stopping after round ${round} of ${ROUNDS}`)
    break
  }
}

// ---------------------------------------------------------------------------------------------------------------------
// Report: root-cause grouping and promotion to regression tests

phase('Report')
let report = null
if (confirmed.length || perfNotes.length) {
  report = await agent(`${CONTEXT}

Write the report of fuzzing run ${RUN} in plain, simple English. Return it in the "markdown" field. Do not write it to a
file: subagents may not write report files, so the main session saves it to ${WORK}/report.md.

Confirmed findings (verification and reduction results):
${compact(confirmed)}

Performance notes (over-invalidation, not correctness): ${compact(perfNotes.slice(0, 20))}

1. Group the confirmed findings by root cause: the same missed dependency is one bug even if the symptoms differ.
   Read the reduced scenarios and the IC code in ${IC_SRC} to decide.
2. For each bug, promote its smallest reproducer to a regression test in the format of ${CORPUS}, under
   ${WORK}/promoted/<group>/<camelCaseName>/: main uses @Test fun runTest() with assertEquals, where the expected values come
   from the clean build output (steps/<n>/clean.stdout of the reduced run); add "// DISABLE_NATIVE: targetFamily=MINGW" and
   cache directives (added cache / modified cache / unchanged cache) that a correct IC would satisfy. Run the harness on
   it: it must show the same finding (the IC binary fails the assertion). Do not copy anything into ${CORPUS}.
3. For each bug write: symptom, minimal steps, the missed dependency with file:line in ${IC_SRC}, a proposed YouTrack title,
   and the promoted test dir. Then a short section on the perf notes, and campaign stats.`,
    { label: 'report', phase: 'Report', schema: REPORT_SCHEMA })
} else {
  log('No confirmed findings: skipping the report agent')
}

const verdictCounts = {}
history.forEach(h => { verdictCounts[h.verdict] = (verdictCounts[h.verdict] || 0) + 1 })
return {
  run: RUN,
  workDir: WORK,
  distStale: prep.stale,
  targets: history.length,
  verdicts: verdictCounts,
  coverage: [...seenFeatures],
  confirmed: confirmed.map(c => ({ id: c.target.id, kind: c.verify.kind, signature: c.verify.signature, reduced: c.reduce && c.reduce.reducedDir })),
  perfNotes: perfNotes.length,
  // The script has no file access: the caller saves this to `${WORK}/report.md`.
  reportMarkdown: report ? report.markdown : null,
  bugs: report ? report.bugs : [],
  history,
}
