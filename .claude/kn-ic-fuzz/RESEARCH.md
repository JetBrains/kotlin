# LLM compiler fuzzing: state of the art, and what we use for K/N IC

Written in November 2026. A research subagent collected the sources. I spot-checked ClozeMaster, AFuzz and icemaker against their pages; the rest are as the agent reported them. The bug counts are the authors' own numbers.

## The short version

The 2025-2026 recipe for LLM compiler fuzzing:

1. **Start from bug history and existing tests.** Do not start from a blank prompt. Seen in FuzzGPT, ClozeMaster, FeatureFuzz, Mut4All, IssueMut.
2. **Use the LLM where it is strong:**
   - **writing generators or mutators offline**, then running a cheap engine (MetaMut, Mut4All, LegoFuzz, Once4All);
   - **reading the code under test** to find where it could break (WhiteFox, KernelGPT, AFuzz).
3. **Use feedback.** Steer by coverage gaps (GapForge, FunFuzz, CovRL-Fuzz). Repair invalid programs from the compiler errors (ReFuzzer).
4. **Use a strong oracle, then reduce and dedup.** Reduce with C-Reduce, Perses, ReduKtor, or LPR, which uses an LLM. Dedup by stack trace or by bisection (BugLens).
5. **Always compare with a plain mutation baseline.** Kitten mutates existing compiler test suites with no LLM, and it beat Fuzz4All.

**For incremental compilation, research is behind practice.** I found no paper on LLM fuzzing of IC. The working tools are:

- **icemaker** (Rust): changes code between incremental runs with tree-splicer, and checks for crashes and stable compilation;
- **rustc `-Zincremental-verify-ich`**: re-hashes a sample of cached query results and reports an ICE on a mismatch;
- **mypy**: runs each test with and without the cache;
- **the Kotlin JVM IC tests**: compare with a clean rebuild in `rebuildAndCompareOutput`.

The K/N IC test fixture checks cache expectations and program asserts, but it does not compare with a clean rebuild.

## Sources by idea

| Idea | Work | Result | How kn-ic-fuzz uses it |
|---|---|---|---|
| LLM as a universal generator and mutator, with a prompt distilled from docs | Fuzz4All, ICSE'24 ([arXiv](https://arxiv.org/abs/2308.04748)) | 98 bugs, 64 confirmed | The generator agent mutates passing scenarios. |
| Bug-triggering history in the prompt | FuzzGPT, ICSE'24 ([arXiv](https://arxiv.org/abs/2304.02014)); ClozeMaster, ICSE'25 ([arXiv](https://arxiv.org/abs/2605.00413)); FeatureFuzz ([arXiv](https://arxiv.org/abs/2601.12360)) | 76; 27; 122 bugs | The Distill "history" agent. The planner prefers targets next to findings. |
| LLM writes mutators from bug reports | Mut4All ([arXiv](https://arxiv.org/abs/2507.19275)); IssueMut, MSR'26 ([arXiv](https://arxiv.org/abs/2510.07834)); MetaMut, ASPLOS'24 ([repo](https://github.com/icsnju/metamut)) | 62 Rust and 38 C++; 65; 130+ bugs | The fix history becomes edit classes (edit kind × topology × sequence shape). |
| Read the code under test, derive trigger conditions, then generate | WhiteFox, OOPSLA'24 ([arXiv](https://arxiv.org/abs/2310.15991)); KernelGPT, ASPLOS'25 ([arXiv](https://arxiv.org/abs/2401.00563)); AFuzz ([arXiv](https://arxiv.org/abs/2605.10074)) | 96; 24; about 40 V8 bugs | The Distill "code" agent reads `CacheBuilder`, the dependency tracker and the fingerprints. |
| Coverage gaps in the prompt | GapForge ([arXiv](https://arxiv.org/abs/2607.15762)); FunFuzz ([arXiv](https://arxiv.org/abs/2605.02789)); CovRL-Fuzz, ISSTA'24 ([arXiv](https://arxiv.org/abs/2402.12222)) | more GCC lines covered; 58 JS bugs | The cheap version: IC features parsed from `-verbose` (`dirty-propagated-cross-library`, `removed-file`, ...) steer the planner. No line coverage yet. |
| Repair invalid inputs from compiler errors | ReFuzzer ([arXiv](https://arxiv.org/abs/2508.03603)) | validity 47% → 97% | Up to 3 repairs per scenario. |
| A plain mutation baseline | Kitten, ISSTA'25 tool ([page](https://conf.researchr.org/details/issta-2025/issta-2025-tool-demonstrations/8/Kitten-A-Simple-Yet-Effective-Baseline-for-Evaluating-LLM-Based-Compiler-Testing-Tec)) | beats Fuzz4All | `harness.py corpus --roundtrip --variant unbatched`. The planner's `mutate-corpus` and `combine` strategies. |
| Program plus an edit list, mutated separately | LSPFuzz, ASE'25 ([arXiv](https://arxiv.org/abs/2510.00532)) | 51 bugs | A scenario is a project plus steps. The mutations change one or the other. |
| Edits between incremental runs | icemaker ([repo](https://github.com/matthiaskrgr/icemaker)); cargo-incremental ([repo](https://github.com/nikomatsakis/cargo-incremental)) | 1,120+ Rust crash reports (not all IC) | The same idea, with a full differential oracle. |
| Typed program generation for Kotlin | Hephaestus, PLDI'22 ([repo](https://github.com/hephaestus-compiler-project/hephaestus)); type-centric fuzzing, ICST'21 ([arXiv](https://arxiv.org/abs/2012.06382)); evolutionary K1/K2 fuzzing, FSE'24 ([arXiv](https://arxiv.org/abs/2401.06653)) | 156; 50+; several | Not used. The LLM writes small, valid, targeted programs instead. |
| Reduction | ReduKtor, ASE'19 ([arXiv](https://arxiv.org/abs/1909.07331)); LPR, ISSTA'24 ([arXiv](https://arxiv.org/abs/2312.13064)) | 25% smaller than Vulcan | `harness.py reduce` (steps, edits, files), then an LLM pass on the code. |
| Dedup | BugLens, ISSTA'26 ([arXiv](https://arxiv.org/abs/2506.23281)) | 27% less triage | The signature is the root-cause label from the verifier. The report agent groups the findings. |

I found no LLM-based Kotlin compiler fuzzer, and no empirical study of IC bugs.

## Why the IC setting suits LLMs

- **The oracle checks itself.** A clean build of the same sources is the reference, so the scenario needs no hand-written expected output. The usual false positives of LLM fuzzers (invalid programs, undefined behaviour) do not apply: an invalid program fails in both builds and is filtered out. The remaining noise is nondeterminism. The harness handles it:
  - it runs the clean binary twice;
  - it normalizes paths, addresses and timings;
  - `--self-check` compares two clean builds.
- **Few, precise programs work better than many random ones.** Each run costs 20-90 seconds of compiler time. So the gain is a program that targets one dependency edge. That is what an LLM is good at, and a grammar-based generator is not.

## How well the harness finds known bugs

I ran the existing corpus on an older prebuilt compiler, `2.5.0-dev-6460` (from 2026-09-07). The harness used only its own oracle: no cache expectations, no asserts. It flagged 5 of the 7 bugs that were fixed later:

| Case | What the harness reported |
|---|---|
| `basic/renamedFileWithWeakDependency` (KT-89876) | IC-only crash in `CachedLibraries$Cache$PerFile.getFileDependencies` |
| `ungrouped/companionBlocksToggled` (KT-89390) | Output mismatch |
| `dependencies/consistentPartialLinkageWarnings` (KT-78253) | The IC link hides a partial linkage error |
| `members/classInterfaceOpenFunToFinal` (KT-87777) | IC-only undefined symbol |
| `members/openClassToFinal` (KT-87777) | IC-only undefined symbol |

The two misses were `obsoletePartialLinkageWarnings` and `openFunToFinalSpecializedFakeOverride`. Both probably came after the bug was already fixed in that build: the latter was an "additional reproducer" commit.

On the current dist, all 98 corpus cases now `PASS`. Getting there, the first corpus run found a few harness bugs. I fixed them:

- `CompilationErrorException` was treated as a crash;
- cinterop modules were built with the default platform libraries;
- `bitcode_deps` of a file that IC kept on purpose was counted as stale. The corpus expects `unchanged cache:` for that file, so this is now an info-level `DEPS_DRIFT`.

## The first campaign

On 2026-11-01 the workflow ran with the defaults on the HEAD dist: 17 agents, about 2.2M tokens, about 98 minutes. It found 6 IC bugs. I reran each one from scratch, outside the workflow. Each bug now has a failing test in `native/native.tests/testData/caches/ic`:

| Test | What IC misses |
|---|---|
| `members/finalClassAddsOverride` | A final class gets its own override. The direct calls to the old fake override depend only on the base class file. |
| `classKind/supertypeRemoved` | A class drops a supertype. A folded `is` check records no dependency. |
| `classKind/valueClassTypeOnlyUse` | class -> value class. A file that uses the class only as a type records no dependency. |
| `dependencies/addedExternalDependencyWithInlineChange` | A library is fully rebuilt (a changed dependencies fingerprint, `-Xcompanion-blocks`), and its dependents are not made dirty. |
| `dependencies/dependentLibraryLeavesAndReturns` | A library leaves the link while its dependency changes, then comes back. |
| `dependencies/weakDependencyOnRemovedInterfaceMethod` | A weak edge from a changed file is not followed. This needs a klib that is not recompiled. |

Four of the bugs came from targets that the planner chose from the Distill hypotheses. Two more (the first two rows) came from probes that the code-reading agent ran on its own. The verifier agents were right every time. But they could not run the reproducers in the real test fixture, and their logs were confusing in places (the same variant passed twice). So check every finding yourself before you report it.

## The second campaign

On 2026-11-06/07 the workflow ran with three rounds of eight targets on a dist freshly built from HEAD (53 agents, about 5.4M tokens, about 5.2 hours). That dist was the first one to contain the call graph building optimizations, the new Obj-C fake-override entry point rules and the partial linkage early-abort removal, so the campaign aimed part of its focus at those. Every run used both the `default` and the `unbatched` variants.

The 24 targets produced 10 confirmed findings, which group into 5 root causes: two new bugs, and three new windows of the open bugs from the first campaign. Each one now has a failing test in `native/native.tests/testData/caches/ic`, re-verified from scratch outside the workflow:

| Test | What IC misses |
|---|---|
| `members/finalClassAddsOverrideCallerInBaseFile` | A devirtualized call whose target lives in the caller's own file records no dependency at all. A sibling of `members/finalClassAddsOverride`. |
| `classKind/valueClassTypeOnlyUseCrossLibrary` | A type-only use across libraries: class -> value class leaves a not-recompiled dependent library with the boxed ABI, and the first call segfaults. A cross-library sibling of `classKind/valueClassTypeOnlyUse`. |
| `classKind/supertypeRemovedCrossLibrary` | A folded `is` check in a library that is not even recompiled when the subtype relation is removed. A cross-library sibling of `classKind/supertypeRemoved`. |
| `dependencies/renamedFileWhileDependentOutOfLink` | A file renamed while the dependent library is out of the link: the removed-file walk sees only linked libraries, the dependent keeps a dangling per-file dependency, and its return crashes the link in `CachedLibraries$Cache$PerFile.getFileDependencies`. KT-89876 fixed the in-link case. |
| `dependencies/partialLinkageIssueOutlivesFix` | A partial linkage issue stored in a per-file cache outlives the fix: the stubbed reference records no bitcode edge, nothing rebuilds the file when the symbol returns, and the replayed issue fails the IC link. Violates the invariant documented in `CacheStorage.savePartialLinkageIssues`. |

The same mechanisms have more shapes than the five tests pin down: the devirtualized-call bug also shows up through a property with a backing field (a stale field offset) and through a suspend call of an interface default, and the crash bug also triggers through a weak trampoline edge. Those reproducers are reduced under the campaign's `build/kn-ic-fuzz/c2-2026-11-06/` output.

No over-invalidation problems appeared: `NOOP_REBUILD` and `UNSTABLE_FINGERPRINT` never showed up in any run. One latent wart is worth a look: an unbatched link writes `bin/bitcode_deps` in a different order than a batched clean link. The dependencies are the same set, but the order difference makes latent cache comparisons noisy.

## The third campaign

On 2026-11-07 the workflow ran with three rounds of eight targets on the same dist (56 agents, about 5.9M tokens, about 3.5 hours). The 24 targets produced 10 confirmed findings, which group into 5 root causes: three new bugs, one independent reproducer of a known open bug, and one reconfirmation of a known open bug. The corpus now has four more failing tests:

| Test | What IC misses |
|---|---|
| `inline/privateClassDefaultCtorStubLinkage` | Not a staleness bug: the per-file cache emits the synthetic `$default` constructor stub of a private class with internal LLVM linkage when the class is reachable from an inlined function, so a friend module's from-scratch IC link fails with an undefined symbol. An uncovered window of the open KT-54330. |
| `classKind/underlyingTypeChangeTypeOnlyUse` | A value class changes its underlying type (Int -> Long). A file that uses it only as a type is never rebuilt, and the stale Int-ABI object layout links under identical mangled names: a silent miscompile. |
| `dependencies/issueOutlivesFixViaDefaultArgumentBridge` | An independent reproducer of `dependencies/partialLinkageIssueOutlivesFix` through a default-argument bridge: the poisoned per-file cache is written even when the step's link fails. |
| `ungrouped/fullRebuildSkipsRemovedFileBookkeeping` | A full cache rebuild skips removed-file bookkeeping, so a renamed-away file's cache directory survives. The leftover directory also corrupts the staleness check, which reads the metadata of an arbitrary directory (`firstOrNull()`); those output-level manifestations depend on the build layout, so the test pins the deterministic bookkeeping half. |

The campaign also produced a 5-line reduction of the known `dependencies/renamedFileWhileDependentOutOfLink` crash, and the first perf findings of the three campaigns: seven `NOOP_REBUILD` / `UNSTABLE_FINGERPRINT` notes, all from the leftover-directory family — the arbitrary-dir staleness sampling keeps rebuilding caches that a correct IC would consider up to date. Fixing the bookkeeping and the sampling should remove them too.

One gap in the harness oracle showed up: its clean reference link also enables incremental compilation (with a fresh cache), so a from-scratch IC-only defect — the private-class stub linkage above — fails both of its links and is reported as `INVALID`. The campaign compensated with manual no-IC comparisons. A third, truly non-IC link per step would turn this family into ordinary `BUG` verdicts.

## The fourth campaign

On 2026-11-07 the workflow ran a fourth time with three rounds of eight targets on the same dist (70 agents, about 6.1M tokens, about 4.9 hours). The 24 targets produced 19 confirmed findings — every target but five — which group into 8 root causes, and all eight are the families that campaigns 1-3 already pinned. The campaign re-derived every known family through new constructs (class and property delegation, operator conventions, lambda captures, folded casts, lateinit accessors), and the hand reductions showed that the fancier shapes collapse onto the plain ones the corpus already tests — which tells the fixer exactly where the missing edges must be recorded. Three windows still pin observables no test pinned, and each is now a failing test:

| Test | What it pins |
|---|---|
| `ungrouped/renameInsideFullRebuildDanglesDependentEdge` | The crash half of the full-rebuild bookkeeping skip: a rename inside a flag-forced full rebuild leaves another library's cached dependency dangling, and the link crashes in `getFileDependencies`. `dependencies/addedExternalDependencyWithInlineChange` documents this crash in prose; `ungrouped/fullRebuildSkipsRemovedFileBookkeeping` pins the leftover-directory half. |
| `inline/privateClassTypeInfoInInlineIsCheck` | Another KT-54330 window: a private class referenced only as a type inside an inlined `is` check keeps internal typeinfo linkage, so the friend's from-scratch IC link fails. `trackCallees` has no `IrTypeOperatorCall` visitor; the campaign also reduced reified-type-argument and catch-clause windows, each with its own visitor gap. |
| `classKind/supertypeRemovedFoldedCasts` | The `as?` and `as` kinds of the folded-check family, whose folds go through different lowering paths than the pinned `is` check. |

No over-invalidation problems appeared. The saturation signal is now clear: this campaign's 19 findings contain no new root cause, its round 3 produced no new coverage features, and every construct the planner invented landed on a known family. Two levers remain for a fifth campaign: the harness oracle gap (its clean reference also enables IC, so from-scratch IC-only defects like the KT-54330 windows are reported as `INVALID` unless an agent runs a manual no-IC comparison — a third, truly non-IC link per step would fix that), and feedback that steers by what has already been found: with every family now carrying multiple reproducers, the planner's do-not-re-derive list is long enough that a fresh campaign is best run after some of the families are fixed, against the fix seams.

*Update, same day:* the oracle gap is closed. The harness now links a truly non-IC build per step and reports from-scratch IC-only defects as ordinary bugs (`IC_ONLY_LINK_FAILURE`, `IC_ONLY_OUTPUT_MISMATCH`); the two KT-54330 corpus tests flipped from `INVALID` to `BUG` on it, and the full corpus kept its shape. The second lever stands: a fresh campaign is still best run against fix seams once some families are fixed, or aimed at the newly visible from-scratch class.

## The fifth campaign

On 2026-11-07 the workflow ran again with three rounds of eight targets (69 agents, about 5.4M tokens, about 3.3 hours), this time against the new oracle. The effect was immediate: 19 confirmed findings, 18 of which fail from a completely fresh cache, and 17 of which are windows of the open KT-54330 family that no earlier campaign could see. They group into four root causes; three became failing tests, and the fourth cannot:

| Test | What it pins |
|---|---|
| `inline/valueClassDefaultCtorStaticStub` | The `$default` stub window crosses the value-class static constructor: `linkageOf` propagates `isCalledFromExportedInlineFunction` through neither `defaultArgumentsOriginalFunction` nor `Autoboxing`'s static-constructor copy. |
| `ungrouped/objectLiteralPrivateSuperclass` | A new root cause, no YouTrack issue: `class_fields` of private classes are serialized under an identity-based `FileLocalSignature` that no consumer session can match, so an inlined object literal over a private superclass crashes the IC build in `ClassFieldsDeserializer`. |
| `dependencies/propertyTypeChangedWhileDependentAbsent` | The leave-and-return family needs no inline body: a declared-type change renames the getter symbol while the dependent is absent, and its stale archive fails the link on return. |

The fourth root cause is the largest: fourteen findings in the `trackCallees` marking gap, of which the smallest was a class literal in an inlined body (one module, two files, no module boundary at all — reduced under `build/kn-ic-fuzz/c5-2026-11-07/promoted/trackCalleesMarking/`). It cannot become a corpus test: the test fixture compiles with IR validation (`-Xphases-to-validate-after=all`), and `IrInlineDeclarationChecker` rejects the class-literal shape with `IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING` — while accepting the constructor-call and is-check shapes that the committed tests pin. The validator covers exactly the shapes `trackCallees` misses, and vice versa, so a KT-54330 fix that marks class literals in inlined bodies will not even compile them under CI validation until the checker is reconciled. The other windows of the gap (is/cast operands, reified type arguments, catch filters, reflection KType payloads, object/SAM supertypes, default-value expressions) are reduced under `build/kn-ic-fuzz/c5-2026-11-07/`.

The other staleness families stayed saturated: the only staleness finding of the campaign is the leave-and-return window above, and no perf findings appeared. The KT-54330 family is now the largest vein in the corpus — three committed tests pin three of its fix surfaces (the regular-class `$default` stub, the is-check typeinfo, the value-class static `$default` stub), and the reduced reproducers cover the rest. It is the clearest candidate for the next fix.

## What is not done yet (good next steps)

1. **Add a clean-rebuild comparison to `AbstractNativeIncrementalCompilationTest`, like the JVM IC tests have.** Then every corpus case checks more than its hand-written expectations.
2. **Use real line coverage of `CacheBuilder` and the dependency tracker as feedback** (as in GapForge): run the compiler JVM with a JaCoCo agent and show the planner the uncovered lines.
3. **Replay the git history of real K/N projects** (the cargo-incremental idea) as a source of realistic edit lists.
4. **Add a compiler-side self-check, like rustc's `-Zincremental-verify-ich`.** When a per-file cache is reused, recompute a sample of fingerprints and dependencies, and fail on a mismatch.
