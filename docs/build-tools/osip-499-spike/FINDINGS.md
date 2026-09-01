# OSIP-499 — JPS on the Build Tools API: spike findings

> **Superseded in part.** The spike loaded the implementation from the `kotlinc` dist, and the notes below
> describe that. The dist no longer carries it: the IDE resolves `kotlin-build-tools-impl` for the Kotlin
> version of the project and passes the directory in `kotlin.jps.btaImplHome` (KT-88958). So the hand-listed
> closure in item 4, the dist rows of the verification table, and the open item about the size of
> `kotlin-dist-for-ide` no longer apply. Items 1, 2, 3, 5 and 6, and the cancellation section, still do.

Companion to [`../jps-integration-required-api.md`](../jps-integration-required-api.md).
Records what the spike actually learned. The code it describes lives in the WIP commit this file was committed
alongside; the entry point is `KotlinJvmModuleBuildTarget.compileModuleChunkWithBuildToolsApi`.

> **Provenance.** The spike produced six working documents (an implementation plan, two adversarial reviews and
> three exploration reports). They were lost when the session scratchpad was cleaned before they could be
> copied here. This file and `PLAN.md` are reconstructed from the session record. The findings below come from
> the implementer's own report and are high-confidence; the exploration reports are gone.

## Shape of the integration

| Decision | Rationale |
| --- | --- |
| **In-process, not daemon** | `getIcOptionsOrNull` hard-`check`s that client-managed IC is not used on the daemon path; only `COMPILER_LOOKUP` crosses BTA's RMI facade. Cost: compiler heap moves into the JPS build process and daemon reuse is lost — a productisation problem, not a spike problem. |
| **Single-target chunks only** | `-Xbuild-file` is a Restricted BTA argument (error since 2.5.0) and absent from the public argument model. Multi-target (circular) chunks fall back to the legacy path. |
| **Glue in `jps/jps-plugin/.../compilerRunner/btapi/`** | No new Gradle module: one would need `CompilerModules`, two `prepare/` projects, `settings.gradle.kts`, `domains.yaml` and both dump files, for no benefit. |
| **Keep JPS's existing tracker `*Impl` instances** | Thin BTA adapters forward into them, so every downstream consumer keeps its downcasts untouched. This was the main diff-size lever. |
| **Flag `kotlin.jps.useBuildToolsApi`, default off** | The legacy path stays the default and the fallback. |

## Where the code contradicted the plan

These are the reusable output of the spike — none is recoverable from the diff alone. The plan had been through
two rounds of code-verified adversarial review and still got each of these wrong.

1. **Classpath must not exclude the chunk's own output dirs while IC is on.**
   `KotlinModuleXmlBuilder.processClasspath` drops them only when IC is *off*
   (`directoriesToFilterOut.contains(file) && !isIncrementalCompilation`). Excluding them unconditionally —
   which is what module.xml appears to do at a glance — broke **130 of 374** incremental tests: with the output
   dir off the classpath it is never indexed, so each incremental round resolved against absent declarations.

2. **`-no-jdk` discards `-jdk-home`.** `configureJdkHome` returns early on `noJdk`, leaving `java.lang.Object`
   unresolvable. The module.xml path sets `JDK_HOME` from `module.modularJdkRoot` *regardless* of `-no-jdk`.
   Correct derivation: `noJdk = (modularJdkRoot == null)`.

3. **`-Xallow-no-source-files` is required.** An incremental round with only *removed* files has zero sources;
   with `buildFile == null` the frontend bails with "No source files" and never rewrites
   `META-INF/<module>.kotlin_module`.

4. **The isolated classpath is 10 jars, not 6.** Beyond impl / cri-impl / compiler-embeddable /
   compiler-runner / tooling-core / stdlib, it also needs `kotlin-reflect`, `kotlin-daemon-client`
   (`JvmCompilationOperationImpl.targetPlatform` is a daemon-common type, initialised eagerly even in-process),
   `kotlinx-coroutines-core-jvm`, `kotlin-script-runtime` and `annotations-13.0`.

5. **`CompilerTargetId` never round-trips.** Without `-Xbuild-file` the CLI builds one module hard-typed
   `"java-production"` (`configureModuleChunk`), so a JPS *test* target's `TargetId(name, "java-test")` comes
   back mismatched and `IncrementalCompilationComponentsImpl` throws. The adapter therefore ignores the
   requested id — sound only because this path is single-target-only.

6. **Diagnostics need two channels.** Argument-validation errors and restricted-argument violations go to the
   `KotlinLogger`, never through `COMPILER_MESSAGE_RENDERER`.

Items 1, 2 and 5 are the ones worth carrying into any productisation discussion: each is a place where the
module.xml path has behaviour that is not expressible through plain compiler arguments.

## Cancellation: JPS pulls, BTA pushes

The one place where the two models are genuinely incompatible, rather than merely inconvenient.

**How the legacy path does it.** The source of truth is `CompileContext.cancelStatus`, flipped by the IDE, and
it is consumed on two independent levels:

- *Engine level.* `CompileContextImpl.checkCanceled()` throws `StopBuildException` (confirmed by decompiling
  `jps-build-261.24374.151.jar`); `IncProjectBuilder` calls it around chunks and rounds, and `KotlinBuilder`
  calls it once itself, before the IC analysis phase (`KotlinBuilder.kt:528`). `KotlinBuilder` rethrows
  `StopBuildException` explicitly (`:364-366`) *ahead* of its catch-all `Throwable -> ABORT`, so a cancellation
  is not reported as a Kotlin build failure.
- *Compiler level.* `KotlinModuleBuildTarget.makeServices` registers a `CompilationCanceledStatus` that polls
  the same flag (`KotlinModuleBuildTarget.kt:281-284`).

Both boundaries are crossed **by pull**. In process, the status object itself travels: the interface and its
exception are forced parent-loaded across the JPS classloader (`KotlinBuilder.kt:99-100`) and
`AbstractCliPipeline.kt:54-55` hoists it into `ProgressIndicatorAndCompilationCanceledStatus`. On the daemon it
is exported over RMI (`JpsCompilerServicesFacadeImpl.kt:34`) and polled *back* from inside the compiler by
`RemoteCompilationCanceledStatusClient`, at most once per 100 ms, counting RMI failures as cancellation so a
dead JPS process also stops the compile. Note that JPS passes no `compilationId` to `daemon.compile`
(`JpsKotlinCompilerRunner.kt:161-168`), so the daemon's push-side `cancelCompilation` machinery is unused by
the legacy path entirely.

Nothing propagates outward: `AbstractCliPipeline.kt:82-85` catches the exception, reports INFO
`"Compilation was canceled"` and returns `ExitCode.OK`. The build aborts only at the *next* `checkCanceled()`.

**What BTA offers.** Push only. `cancel()` sets a flag that `cancellationHandle` observes
(`CancellableBuildOperationImpl.kt:36-40`); on the daemon it is an out-of-band
`daemon.cancelCompilation(sessionId, compilationId)` (`BaseCompilationOperationImpl.kt:199-201`). A client
cannot hand in a cancellation source, and the operation builds its own `Services`, so JPS's status is simply
never consulted.

**The bridge in the spike.** `withCancellationWatchdog` (`JpsBtaCompilerRunner.kt:243-272`) starts a daemon
thread that polls `context.cancelStatus` every 200 ms and calls `operation.cancel()`;
`OperationCancelledException` is swallowed at `:95-97` so JPS aborts through its own `checkCanceled()`, exactly
as before. This is why `testCancelKotlinCompilation` is not among the flag-on failures.

It is correct, but it is in the wrong place: every pull-based build system embedding BTA has to write the same
thread. It also costs one thread per chunk, adds up to 200 ms of latency, and a `cancel()` that lands just
after the compile finished still discards a successful result, because `executeImpl` re-checks the flag after
execution returns.

**What BTA would need instead** — a way to accept a cheap "am I cancelled?" callback:

```kotlin
public fun interface CancellationChecker {
    /** Must be cheap and non-blocking. Called very often during compilation. */
    public fun isCancelled(): Boolean
}

// on CancellableBuildOperation
@JvmField
public val CANCELLATION_CHECKER: Option<CancellationChecker?> =
    Option("CANCELLATION_CHECKER", KotlinReleaseVersion(2, 5, 0))
```

with the whole watchdog collapsing to:

```kotlin
operation[CancellableBuildOperation.CANCELLATION_CHECKER] =
    CancellationChecker { context.cancelStatus.isCanceled }
```

Design notes:

- **A `Boolean`, not a throwing `checkCanceled()`.** `CompilationCanceledStatus` and
  `CompilationCanceledException` are compiler-internal types; keeping them out of the API is the point of the
  exercise. The impl wraps the checker into its own status and throws internally.
- **An `Option`, not a constructor parameter,** so it is version-gated by `availableSinceVersion` like every
  other option and old clients are unaffected.
- **Result semantics unchanged:** still `OperationCancelledException` out of `executeOperation`, so push and
  pull clients see the same outcome.
- **In process it is nearly free:** one extra condition in `cancellationHandle`. It sits on a very hot path, so
  either document that the checker must not block, or throttle it the way
  `RemoteCompilationCanceledStatusClient` already does.
- **On the daemon, move the watchdog into the impl:** poll the checker and call the existing
  `cancelCompilation`. The daemon protocol stays as it is, and every pull-based client gets the behaviour for
  free.

## Verification reached

| Step | Result |
| --- | --- |
| `./gradlew dist` + sha256 against `build/libs` | all 6 jars hash-match |
| JPS suites, flag **off** | 435 tests, **0 failures** — the existing path is unaffected |
| JPS suites, flag **on** | 435 tests, **4 failures**; all 374 incremental tests pass |
| `installJps -Ppublish.ide.plugin.dependencies=true` | succeeded; `kotlin-dist-for-ide` carries the full closure |
| Build `jps-playground` from the IDE | **never run** |

Remaining flag-on failures: three `SourcePackagePrefix` variants (the predicted `JvmSourceRoot.packagePrefix`
casualty — no CLI equivalent) and `testHelp` (BTA drops `-help`, so usage text is never produced).
Predicted casualties that did **not** materialise: `testDaemon`, `testCancelKotlinCompilation`, `Jre11`,
`CircularDependencies*`.

## Open items

- **Cancellation** is the reason this work was set aside. The polling watchdog works, but the fix belongs in
  BTA rather than in each client — see [Cancellation: JPS pulls, BTA pushes](#cancellation-jps-pulls-bta-pushes).
- **`setupIncrementalCompilationServices` regression, unrelated to this spike and deliberately untouched.**
  The preceding commit deleted the `if (!incrementalCompilationIsEnabled(arguments)) return` gate in
  `compiler/cli/cli-jvm/.../JvmConfigurationPipelinePhase.kt`, so five services now register on *every* JVM CLI
  compilation, for all consumers, untested. Note that `docs/build-tools/jps-integration-required-api.md`
  describes this incorrectly, claiming the in-process path forces `incrementalCompilation = true` instead.
- **`kotlin-compiler-embeddable.jar` is ~189 MB**, so shipping the impl closure roughly doubles the
  `kotlin-dist-for-ide` download. Fine for a spike; a real question for productisation.
- `docs/build-tools/jps-integration-required-api.md` also dates the `-Xbuild-file` error to 2.6.0; the code
  says `errorSince = v2_5_0`.
