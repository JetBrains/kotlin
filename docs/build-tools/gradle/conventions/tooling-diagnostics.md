Working with the Tooling Diagnostics framework
===

Diagnostics are how the Kotlin Gradle plugin (KGP) reports warnings and errors to build authors: a severity, an actionable message with solutions and links, visible in the Gradle Problems report and the IDE, suppressible per id. Diagnostics are declared in `KotlinToolingDiagnostics.kt` (module `kotlin-gradle-plugin`); severities live in `kotlin-gradle-plugin-api`. This document covers *when* to report a diagnostic and how to make it useful; for the API itself — declaring, reporting, testing — see the KDoc next to the code.

## Diagnostic, log message, or exception?

**Can the build author act on the message?**

- **Yes — their misconfiguration, deprecated usage, or invalid input** → report a diagnostic.
- **No — KGP-internal state, useful only for debugging** → log a message, or nothing.
- **No — a broken internal invariant, i.e. a KGP bug** → throw: a diagnostic implies the user can fix it; they can't.
- **Failing is the task's job** (tests pass, lockfile matches) → fail the task with an exception: the failure is the *result*, not a misconfiguration.
- **The build would crash moments later anyway, with a worse message** → report a `FATAL` diagnostic: replace the obscure `LinkageError` with a structured, fixable message.

Never report a diagnostic *and* throw for the same problem.

DOs and DON'Ts:

- **DO** migrate actionable `logger.warn` messages to diagnostics — logs are easy to miss and can't be suppressed; diagnostics reach the Problems report and the IDE.
- **DON'T** downgrade a problem to a log message because it "isn't that bad" — if it's actionable, it's a diagnostic at a lower severity.
- **DON'T** turn task-contract failures into diagnostics — the user asked the task to fail.
- **DO** report a `FATAL` diagnostic before an inevitable crash — the user gets a message they can act on instead of a stacktrace to decipher.

## Should the build fail? Choosing a severity

Ask in this order:

1. **Can the current process continue at all?** If the next step would crash, hang, or corrupt state → **`FATAL`**: stops configuration/sync (or the running task) immediately. Rare by design — it masks every later diagnostic, so users fix one problem per run, and a sync-time abort can leave the IDE without a project model.
2. **Should Kotlin compilation stop?** → **`ERROR`**: configuration completes, sync and non-compiling tasks (`help`, `clean`) still pass — only Kotlin compilation fails, so a build that does something useful without compiling Kotlin still succeeds.
3. **Nothing may fail, but the user must not miss it?** → **`WARNING`**. Use **`STRONG_WARNING`** when only part of the build is broken: it looks and suppresses like an error, but the healthy parts keep compiling and sync stays alive so the user can fix things — the broken parts fail on their own anyway.

Further rules:

- Prefer the least severity that gets the user to act: downgrading later is painless; upgrading `WARNING` → `ERROR` breaks builds that used to pass, so do it only on a deprecation schedule (pass severity to the factory so a diagnostic can graduate).
- Diagnostics are suppressible per id — `FATAL` is not. If your diagnostic needs an escape hatch, it probably shouldn't be `FATAL`.
- At execution time, `ERROR` fails subsequent Kotlin compilations but lets the current task finish; only `FATAL` fails the task immediately. Ask: must *this* task stop right now?

DOs and DON'Ts:

- **DO** use `FATAL` for input that makes every later step meaningless (a circular `dependsOn` graph, duplicate source set names) — continuing only produces cascading errors.
- **DON'T** use `FATAL` for a check that might misfire in exotic environments — an unsuppressible abort punishes users for KGP's uncertainty.

## Report from a checker or in place?

- **Default to a checker** (`KotlinGradleProjectChecker`): checkers run after the build script is fully evaluated, so they see the *final* configuration. If the check needs the whole picture — targets, source set graph, final property values — it's a checker.
- **Report in place when the event itself is the problem**: a deprecated DSL function was called, an experimental property was opted into, the environment can't run KGP.
- **Report in place when the build can't safely continue** — don't wait for a checker (e.g. a cycle found mid-`dependsOn`-walk).
- **From a task action or artifact transform**, report through the execution-time interface (see the `UsesKotlinToolingDiagnostics` KDoc) — it renders immediately.

Checker discipline (each rule learned from a real bug):

- Await the *narrowest* data you need, not "fully configured" — you'll still report when a later stage throws, instead of hiding behind a downstream crash (see the `KotlinGradleProjectChecker` KDoc).
- Only *observe* — eagerly reading lazy state finalizes it and breaks other plugins: this has nulled out build-script properties (KT-83678) and broken cross-project configuration (KT-79315).
- Many targets or source sets triggering the same problem → report once per project; once per build if the message needs no project context.

DOs and DON'Ts:

- **DO** put whole-picture checks in checkers — the answer isn't known until configuration ends.
- **DON'T** run misconfiguration checks eagerly inside `apply {}` — a partially configured project yields noise and false positives.
- **DON'T** let a checker touch configurations or dependencies — it must observe the final model, never finalize or mutate it.

## Writing a useful diagnostic

The message is often the only thing the user sees — it must be understandable and actionable without any other source:

- **Language**: understandable without advanced Gradle knowledge — describe what happened in terms of the user's own build (their targets, properties, build script), not KGP internals or Gradle lifecycle jargon.
- **Title**: short, names the problem, includes the offending item (target, source set, property).
- **Description**: the *consequence* — what breaks and why — not just what was detected. Include the project name/path so users in multi-project builds can tell which module it came from.
- **Solutions**: each a single concrete step, to fix or to investigate. Prefer one; add more only for genuinely distinct fixes.
- Attach a stacktrace when *where it was reported from* matters more than the message (e.g. which DSL call triggered it).

DOs and DON'Ts:

- **DO** say exactly what to do ("rename the duplicated source sets") — the user should never have to reverse-engineer the fix.
- **DO** have a technical writer review the text — they catch what authors gloss over: jargon, unactionable steps, missing links.
- **DON'T** restate the title as the description, or offer "solutions" like "check your configuration" — a step the user can't perform is noise.
- **DON'T** lean on KGP-internal vocabulary (lifecycle stages, internal class names) — the reader is a build author, not a KGP developer.

## Links

- If a documentation page exists, link it → a **`kotl.in` short link** in the `documentationLink` field — it renders as a "see … for more details" line and reaches the Gradle Problems report. Never hardcode deep URLs: short links survive doc reorganizations.
- A known, tracked problem → link the **YouTrack issue** (KT-XXXXX) in the description — it carries workarounds and status.
- "Please report this" → **`kotl.in/issue`**.
