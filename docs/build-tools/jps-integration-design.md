# Integrating the Build Tools API into JPS

Status: exploration spike (OSIP-499). Design agreed; the code it produces is throwaway.

## Context

The JPS Kotlin builder invokes the compiler through machinery that predates BTA and duplicates
what BTA now provides: it generates a `module.xml` descriptor, passes it via `-Xbuild-file`, and
talks to the Kotlin compile daemon over RMI (`JpsKotlinCompilerRunner`), with a reflective
in-process fallback through a hand-rolled preloading classloader (`CompilerRunnerUtil`).
Diagnostics come back either over an RMI services facade or by parsing the compiler's XML stdout.

Every other build system integration — KGP (`libraries/tools/kotlin-gradle-plugin/.../btapi/`) and
Maven (`K2JVMCompileMojo.java`) — has moved to BTA. JPS is the last consumer on the legacy path,
and it is the one carrying the most bespoke code. Moving it to BTA deletes the `module.xml`
generator, the RMI facade, and the preloader classloader, and gives JPS the same
classloader-isolated compiler that everyone else gets.

This document covers a deliberately reduced problem: **Kotlin-only modules with no module
dependencies, in-process execution only.** That is the smallest slice that exercises the full round
trip — arguments in, diagnostics and generated class files out, registered with JPS. Mixed
Java/Kotlin modules, the daemon execution policy, and cyclic module chunks are out of scope and are
enumerated at the end as follow-up work.

Incremental compilation *is* supported, but it is run by the compiler rather than by JPS; see
[Incremental compilation](#incremental-compilation).

### Success criterion

This is a spike, not a mergeable MR, so the bar is a working round trip rather than a green CI
matrix. Concretely: **`~/IdeaProjects/jps-playground` builds from the IDE through the BTA path.**
That project is a plain three-module JPS project (`jps-playground`, `util`, `bta-runner`), Kotlin
only, no cycles — exactly the reduced problem above. It already pins `2.5.255-SNAPSHOT` in
`.idea/kotlinc.xml` (`KotlinJpsPluginSettings`), so it picks up whatever `./gradlew installJps`
publishes to `~/.m2`.

That choice of loop has one consequence worth stating up front: the artifact that matters is
`kotlin-dist-for-jps-meta`, **not** `dist/kotlinc/lib`. See [Classloader and
packaging](#classloader-and-packaging).

### Two constraints that shape everything

1. **`module.xml` cannot survive.** `-Xbuild-file` (and its alias `-module`) is a *restricted
   argument* in BTA — a warning today, a hard `CompilerArgumentsParseException` from 2.5.0
   (`compiler/build-tools/kotlin-build-tools-api-tests/src/testRestrictedArguments/kotlin/RestrictedArgumentsTest.kt`).
   Today `module.xml` carries sources, classpath, output dir, friend dirs, Java source roots and
   the modular JDK root for *every module in the chunk*. Per module, every one of those fields has
   a CLI argument, so it re-expresses cleanly (see [Argument mapping](#argument-mapping)). What
   does not re-express is the *chunk*: **one BTA operation can only describe one module**, which is
   what forces the cyclic-chunk decision below.

2. **Output items now have a clean channel.** JPS must call
   `outputConsumer.registerOutputFile(target, outputFile, sourcePaths)` for every generated class.
   Until commit `7c68a1088079` ("[BTA] Expose compiler OUTPUT messages as a distinct renderer
   severity") this was impossible to do cleanly: `CompilerMessageRendererAdapter` collapsed both
   `CompilerMessageSeverity.OUTPUT` and `LOGGING` into `Severity.DEBUG`. That commit gives `OUTPUT`
   its own case, so a `CompilerMessageRenderer` installed by JPS can recognise output messages and
   parse them with `OutputMessageUtil.parseOutputMessage`. **No further BTA API work is needed.**

## Design

### Seam

`KotlinJvmModuleBuildTarget.compileModuleChunk(...)`
(`jps/jps-plugin/src/org/jetbrains/kotlin/jps/targets/KotlinJvmModuleBuildTarget.kt:100`) is the
narrowest cut. Everything above it — dirty-file computation, chunk handling, output registration,
`ExitCode` policy — is JPS-side and reused unchanged. Everything below it — `module.xml`
generation, `JpsKotlinCompilerRunner`, `CompilerRunnerUtil`, `JpsCompilerServicesFacadeImpl` — is
what BTA replaces.

Gated on the system property **`kotlin.jps.useBuildToolsApi`**, default off, both paths coexisting.
In the IDE it is set per project under *Settings → Build, Execution, Deployment → Compiler →
Shared build process VM options*, which is what makes A/B-ing the same project against both paths
possible.

Toggling the flag invalidates JPS's Kotlin caches in both directions, which it has to: while the
Build Tools API path runs, the compiler owns incremental compilation and JPS's own caches stop
being updated, so on the way back they would be arbitrarily stale and would under-approximate the
dirty set. `isIncrementalCompilationDelegatedToCompiler` is therefore folded into both cache
attribute inputs — the per-target `localCacheVersionManager` (`KotlinModuleBuildTarget.kt:78`) and
the expected lookup cache components (`KotlinCompileContext.makeLookupsCacheAttributesManager`).
Turning the flag on makes them `SHOULD_BE_CLEARED`, turning it off makes them `INVALID`, and JPS
clears and rebuilds accordingly. **A/B comparisons therefore always start from a full compile**,
which is the honest baseline anyway.

```kotlin
// KotlinJvmModuleBuildTarget.compileModuleChunk
if (System.getProperty("kotlin.jps.useBuildToolsApi").toBoolean()) {
    // Every guard reports ERROR, not STRONG_WARNING: that sets Utils.ERRORS_DETECTED_KEY,
    // which makes KotlinBuilder.kt:460 return ExitCode.ABORT.
    if (chunk.targets.size > 1) {
        environment.messageCollector.report(
            ERROR,
            "The Build Tools API path does not support circular module dependencies: " +
                    chunk.presentableShortName
        )
        return false
    }
    if (isIncrementalCompilationEnabled && compileScopeModuleDependencies().isNotEmpty()) {
        environment.messageCollector.report(
            ERROR,
            "The Build Tools API path does not support incremental compilation of a module that " +
                    "depends on other modules."
        )
        return false
    }
    if (sources.crossCompiledFiles.isNotEmpty()) {
        environment.messageCollector.report(
            ERROR,
            "The Build Tools API path does not support multiplatform modules yet."
        )
        return false
    }
    return JpsBuildToolsApiCompilerRunner(...).compile(...)
}
```

The legacy body stays as the `else` branch, unchanged.

All these guards fail the build loudly rather than degrading. For the cyclic case that is a deliberate
choice over silently falling back to the legacy runner: two live compilation paths inside one build
would make any result hard to attribute, which defeats the point of a spike. The module-dependency
guard is not merely informational: incremental compilation is configured to treat the classpath as
unchanged, so a change in a dependency would be missed and the result would be quietly stale rather
than wrong-looking.

### New components

All under a new package `jps/jps-plugin/src/org/jetbrains/kotlin/compilerRunner/btapi/`, mirroring
KGP's layout.

| Class | Responsibility |
|---|---|
| `JpsBuildToolsApiCompilerRunner` | Builds the `JvmCompilationOperation`, executes it, maps the result |
| `JpsBtaToolchainsProvider` | Resolves the impl classpath from `KotlinPaths`, caches `KotlinToolchains` |
| `JpsBtaBuild` | Holds the `KotlinToolchains` and the `BuildSession` opened on them, as one pair, in a `GlobalContextKey`; closed in `buildFinished` |
| `JpsCompilerMessageRendererBridge` | `CompilerMessageRenderer` — routes diagnostics and output items back into JPS |
| `JpsBtaCompilerArguments` | Assembles one `K2JVMCompilerArguments` and flattens it to argument strings |
| `JpsBtaLogger` | `KotlinLogger` over `KotlinBuilder.LOG` that also reports into the *Build* tool window when `kotlin.jps.verbose` is set |

### The diagnostics + output bridge

This is the load-bearing piece. `BaseCompilationOperation.COMPILER_MESSAGE_RENDERER` is the only
hook that receives *structured* `(severity, message, location, diagnosticId)`. The `KotlinLogger`
only ever sees an already-rendered string, which would lose the file/line that JPS needs to make
messages clickable in the IDE. So the renderer does double duty:

```kotlin
class JpsCompilerMessageRendererBridge(
    private val messageCollector: MessageCollectorAdapter,
    private val outputItemsCollector: OutputItemsCollectorImpl,
) : CompilerMessageRendererWithDiagnosticId {

    override fun render(severity: Severity, message: String, location: SourceLocation?, diagnosticId: String?): String {
        if (severity == Severity.OUTPUT) {
            OutputMessageUtil.parseOutputMessage(message)?.let { output ->
                outputItemsCollector.add(output.sourceFiles, output.outputFile)
            }
            return ""
        }
        messageCollector.report(severity.asCompilerMessageSeverity(), message, location?.asJpsLocation())
        return ""
    }
}
```

Returning `""` is load-bearing: `KotlinLoggerMessageCollectorAdapter.kt:34` drops blank renders, so
nothing is reported twice. JPS receives everything through the existing `MessageCollectorAdapter`
→ `context.processMessage(CompilerMessage(...))` path, which is also what flips
`Utils.ERRORS_DETECTED_KEY` and therefore what makes `KotlinBuilder.kt:460` return `ABORT` on
compile errors. `-Xreport-output-files` needs no new plumbing — `KotlinChunk.compilerArguments`
(`KotlinChunk.kt:116-125`) already sets `reportOutputFiles = true`.

Known fidelity loss, worth stating plainly: `CompilerMessageRendererAdapter` folds `EXCEPTION` into
`Severity.ERROR` on the way to the renderer
(`asCompilerMessageRendererSeverity`, `CompilerMessageRendererAdapter.kt:57-74`), so JPS cannot tell
the two apart and loses the `CompilerRunnerConstants.INTERNAL_ERROR_PREFIX` marker it applies today
(`MessageCollectorAdapter.kt:30-32`). Both still map to `BuildMessage.Kind.ERROR`, so the build
still fails correctly; only the message prefix differs. Accept for now, note as follow-up.

That is the *whole* loss, though. The same adapter also collapses `STRONG_WARNING` and
`FIXED_WARNING` into `Severity.WARNING`, but JPS's own `MessageCollectorAdapter.kind()`
(`:68-76`) already collapses them the same way, so nothing observable changes there. Note also that
`warningsAsErrors` is applied by `KotlinLoggerMessageCollectorAdapter.toEffectiveSeverity` *before*
the renderer runs, so under `-Werror` JPS sees promoted warnings as `ERROR` — which is the
behaviour it wants anyway.

Using a renderer as a side-effecting bridge is pragmatic rather than pretty. The clean long-term
shape is a dedicated structured diagnostic/output listener option on `BaseCompilationOperation` —
see follow-ups.

### Argument mapping

Do **not** hand-map `module.xml` fields onto individual typed BTA options. Assemble one
`K2JVMCompilerArguments`, flatten it to CLI strings, and let BTA parse it back through
`CommonToolArguments.applyArgumentStrings(arguments: List<String>)`
(`kotlin-build-tools-api/gen/.../arguments/CommonToolArguments.kt:102`). This is exactly what KGP
does in `JvmBuildOperationFactory.createOperation`
(`libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/compilerRunner/btapi/jvm/JvmBuildOperationFactory.kt:22-32`);
JPS should follow it rather than invent a second style.

The reason this matters is coverage. Hand-mapping only reaches the arguments someone remembered to
map and silently drops the rest, so every new compiler argument becomes a JPS change. Round-tripping
through strings carries **everything** the facet, project settings and `additionalArguments` can
express — including the multiplatform arguments (`-Xcommon-sources`, `-Xfragments`,
`-Xfragment-sources`) and the Java-interop ones (`-Xjava-source-roots`, `-Xjava-package-prefix`).
Nothing has to be skipped for want of a typed option.

```kotlin
// seeded from commonArguments + module.k2JvmCompilerArguments, then filled in by JPS
val args: K2JVMCompilerArguments = ...

val builder = kotlinToolchains.jvm.jvmCompilationOperationBuilder(
    sources.map { it.toPath() },
    outputDir.toPath(),
)
args.destination = null   // -d is restricted in BTA; it is a builder parameter instead
builder.compilerArguments.applyArgumentStrings(flattenArguments(args, compilerSettings))
```

Only two things stay outside the argument strings, because their CLI forms are restricted
arguments: the source list (compiler `freeArgs`) and the output directory (`-d`). Both are
parameters of `jvmCompilationOperationBuilder`. Nulling `destination` after reading it mirrors
KGP's own hack, tracked as KT-85394. `-Xbuild-file` simply is never set — there is no `module.xml`
to point at.

What JPS fills in on the arguments object before flattening:

| `module.xml` today | `K2JVMCompilerArguments` field |
|---|---|
| `outputDir` | builder parameter, not an argument |
| `sources` | builder parameter, from `freeArgs` |
| `classpath`, minus output dirs | `classpath` |
| `friendDir` | `friendPaths` |
| `modularJdkRoot` | `jdkHome` |
| module name / `targetId` | `moduleName` |
| `javaSourceRoots` | `javaSourceRoots` + `javaPackagePrefix` |
| `commonSources` | not mapped — multiplatform modules are rejected, see below |
| implied by JPS | `noStdlib`, `noReflect` = `true`; `noJdk` = `true` only without a `modularJdkRoot` |
| `buildFile` | deleted |

Multiplatform modules are out of scope for the spike and rejected at the seam, in the same way as
circular chunks and modules with dependencies. `collectSourcesToCompile` marks sources that JPS pulls
in from a common module as cross-compiled (`KotlinModuleBuildTarget.kt:313`); compiling those as
ordinary sources instead of passing them as `-Xcommon-sources` would fail with confusing
`expect`/`actual` errors, so a non-empty `crossCompiledFiles` is an `ERROR`. Supporting them later
is a matter of setting `arguments.commonSources` — the round-tripping already carries the argument.

One fidelity gap survives, and only outside this document's scope: `module.xml` carries a
`packagePrefix` *per* Java source root (`JvmSourceRoot`, `KotlinJvmModuleBuildTarget.kt:331-343`),
while `-Xjava-package-prefix` is a single global value. Irrelevant for Kotlin-only modules; it
becomes a real constraint when mixed modules are taken on.

Sources are always the **full** source list of the target (`KotlinModuleBuildTarget.sources`), never
`collectSourcesToCompile`'s result, which with IC on is only the files JPS marked dirty. The
compiler works out the compile set itself and treats anything outside the list it was given as
*removed*, so handing it a subset would silently drop files. `collectSourcesToCompile` is still
called, for its `logFiles()` reporting.

Classpath reproduces `KotlinModuleXmlBuilder.processClasspath`'s behaviour
(`build-common/src/org/jetbrains/kotlin/modules/KotlinModuleXmlBuilder.kt:109-132`): when IC is
off, output directories of the module and its dependencies are **excluded** from the classpath so
stale binaries from a previous build cannot leak in, and **kept** when IC is on. Compute as
`findClassPathRoots() - chunk.targets.map { it.outputDir }.toSet()`, or plain `findClassPathRoots()`
with IC on.

`modularJdkRoot` → `jdkHome` is the one row that does not survive `noJdk = true`, and the two have
to be decided together. `module.xml` carried the modular JDK root on a channel of its own:
`JvmConfigurationPipelinePhase.kt:253-261` puts `module.modularJdkRoot` into
`JVMConfigurationKeys.JDK_HOME` *unconditionally*, after and regardless of `NO_JDK`. The only
command line equivalent is `-jdk-home`, and `CompilerConfiguration.configureJdkHome`
(`jvmArguments.kt:182-190`) short-circuits on `-no-jdk` before setting `JDK_HOME`, warning that
`-jdk-home` is ignored. So:

- **Modular JDK (9+).** `-jdk-home` has to win. It is the only channel by which the JDK reaches the
  compiler: JPS exposes the SDK as `jrt://…!/java.base` URLs, which `findClassPathRoots`
  (`KotlinJvmModuleBuildTarget.kt:477-490`) filters out precisely because they are not real files —
  that filter is why `findModularJdkRoot()` exists in the first place. With `JDK_HOME` unset,
  `CliJavaModuleFinder` has no `jrtFileSystemRoot`, `ClasspathRootsResolver.addModularRoots` finds
  no observable modules and adds nothing, and `java.lang.*` is unresolvable. Dropping `-no-jdk`
  costs nothing in exchange: `configureJdkClasspathRoots` (`JvmContentRoots.kt:117-129`) adds no
  roots for a modular JDK anyway.
- **Non-modular JDK.** There is no `modularJdkRoot`, JPS puts the JDK jars on the classpath itself,
  and `-no-jdk` has to stay — otherwise `configureJdkHomeFromSystemProperty` injects the build
  process's own JDK.

Hence `noJdk = (modularJdkRoot == null)` rather than a constant `true`. KGP resolves the same
tension the same way, by treating the two as mutually exclusive
(`GradleKotlinCompilerRunner.kt:140`).

The flattening step is already written, just in the wrong place. `withAdditionalCompilerArgs`
(`JpsKotlinCompilerRunner.kt:200-207`) runs `ArgumentUtils.convertArgumentsToStringList`, appends
`compilerSettings.additionalArgumentsAsList`, then applies the `-P plugin:` dedup (`:212`) and the
`-Xwarning-level=` dedup (`:240`). All three are `private`. **Extract them into a shared internal
helper** so both runners use one implementation and `JpsKotlinCompilerRunnerTest` keeps covering
it. That helper becomes the single place where "which arguments does JPS pass" is decided, for the
legacy path and the BTA path alike.

### Incremental compilation

JPS and the Kotlin compiler each have a complete incremental engine, and only one can be in charge.

JPS's engine keeps a `JpsIncrementalJvmCache` per target plus a project-wide lookup storage, and
re-runs `KotlinBuilder` in extra rounds until nothing more is marked dirty. Every part of it is fed
by the trackers in the `Services` object — and `Services` is not a Build Tools API concept at all.
The compiler's engine, which BTA exposes as *snapshot-based incremental compilation*, keeps its own
caches in a directory the caller provides, creates its own trackers internally, and iterates
internally within a single `executeOperation` call.

Trying to run both means JPS hands the compiler a dirty-file subset, the compiler narrows that
subset again with its own analysis, and JPS then updates its caches from trackers nothing populated.
So the compiler's engine takes the job outright, and JPS's Kotlin bookkeeping is switched off.

#### Switching JPS's bookkeeping off

Through the existing `hasCaches` extension point, overridden in `KotlinJvmModuleBuildTarget`:

```kotlin
override val isIncrementalCompilationDelegatedToCompiler: Boolean
    get() = JpsBuildToolsApiCompilerRunner.isEnabled && isIncrementalCompilationEnabled

override val hasCaches: Boolean
    get() = !isIncrementalCompilationDelegatedToCompiler
```

`BuildDataManager.getKotlinCache` returns `null` for a target without caches
(`JpsIncrementalCache.kt:66-68`), and every consumer already handles that, so one override disables
the lot: `KotlinChunk.loadCaches` yields an empty map, the complementary-file marking and
`cache.markDirty` in `doCompileModuleChunk` skip themselves, `markDirtyComplementaryMultifileClasses`
`continue`s, and — worth calling out — `markAdditionalFilesForInitialRound` skips too, which is what
stops it invoking the *legacy* `JpsKotlinCompilerRunner` on every incremental build.

That leaves exactly one edit in `KotlinBuilder`, because `updateCaches` reaches into the map with
`!!`:

```kotlin
if (!representativeTarget.isIncrementalCompilationEnabled ||
    representativeTarget.isIncrementalCompilationDelegatedToCompiler
) {
    return OK
}
```

What deliberately stays on: `kotlinChunk.saveVersions()` (otherwise `isVersionChanged` forces a
rebuild every build) and `updateChunkMappings` (it feeds JPS's Java dependency graph from generated
class files, and already ran on the non-incremental BTA path).

`isIncrementalCompilationEnabled` itself must keep reporting `true`. Turning it off would take the
`CHUNK_REBUILD_REQUIRED` branch at `KotlinBuilder.kt:412` on every build with dirty Kotlin files,
which is the opposite of what is wanted.

#### Configuring the operation

```kotlin
snapshotBasedIcConfigurationBuilder(
    workingDirectory = <targetDataRoot>/kotlin-bta,
    sourcesChanges = SourcesChanges.Known(modifiedFiles, removedFiles),
    dependenciesSnapshotFiles = emptyList(),
).apply {
    this[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] = true
    this[FORCE_RECOMPILATION] = isChunkRebuilding
    this[BACKUP_CLASSES] = true
    this[KEEP_IC_CACHES_IN_MEMORY] = true
}
```

- **`workingDirectory`** sits under `dataPaths.getTargetDataRootDir(target)`, next to where JPS's own
  Kotlin caches would live, so both are discarded together with the target's build data.
- **`SourcesChanges.Known`** comes from `KotlinDirtySourceFilesHolder`. Take `dirty.values`, not
  `dirty.keys`: the map is keyed by the *normalised* path but holds the file as JPS found it, while
  `sources` is built from the raw walk. A modified file that is not `File.equals` to a known source
  is classified as *removed* by the compiler and never compiled. The seam asserts the subset relation
  rather than trusting it.
- **Empty `dependenciesSnapshotFiles` with `ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES`** states outright
  that the classpath is not examined; the module-dependency guard is what makes that safe.
- **`FORCE_RECOMPILATION`** mirrors `KotlinBuilder`'s `isChunkRebuilding`, i.e.
  `JavaBuilderUtil.isForcedRecompilationAllJavaModules` or `rebuildAfterCacheVersionChanged`. It is
  also what cleans the output and working directories, so no deletion code is needed here.
- **`BACKUP_CLASSES` + `KEEP_IC_CACHES_IN_MEMORY`** go together: class files are restored and caches
  are only flushed on success, so a failed compilation cannot leave the caches describing outputs
  that were rolled back. JPS no longer keeps a second copy of that state, so this matters more here
  than it does for Gradle.
- **`OUTPUT_DIRS` is deliberately unset.** Its computed default is exactly
  `{ destinationDir, workingDir }`; an explicit value must be a superset of those anyway. The
  destination is known despite the seam nulling `destination`, because the implementation writes it
  back in `createAndPrepareCompilerArguments`.
- **`ROOT_PROJECT_DIR` / `MODULE_BUILD_DIR` unset**, so caches hold absolute paths. They only buy
  relocatability and are all-or-nothing.

The first build after the caches are gone is always a full one:
`<workingDirectory>/shrunk-classpath-snapshot.bin` does not exist yet, which the implementation
checks *before* it looks at `ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES`, so it falls back to a full
compile and writes the snapshot on the way out. Self-healing, but it means a test has to build at
least twice before asserting anything about incrementality.

### Classloader and packaging

`KotlinToolchains.loadImplementation(classpath: List<Path>)` builds a `URLClassLoader` over
`SharedApiClassesClassLoader`, which delegates only `org.jetbrains.kotlin.buildtools.api.*` back to
the API's own loader. That prerequisite is already satisfied: `kotlin-build-tools-api` is in
`kotlinJpsPluginMavenDependencies` (`repo/kotlin-build-helpers/src/CompilerModules.kt:402-409`) and
is on the JPS plugin's runtime classpath today — it is currently used only for the `KotlinLogger`
interface (`JpsKotlinLogger.kt:20`).

What is *not* satisfied is the impl side. `jps.kotlin.home` is where `KotlinPaths` resolves jars by
name, and for an IDE build that directory is materialised from the **`kotlin-dist-for-jps-meta`**
artifact, not from this repo's `dist/`. Verified against the live cache written by the playground:

```
~/Library/Caches/JetBrains/IntelliJIdea2026.2/kotlin-dist-for-ide/2.5.255-SNAPSHOT/lib/
```

Two facts fall out of inspecting it, and together they decide the packaging approach:

- IntelliJ materialises the **full transitive closure** of the meta pom, each jar named
  `<artifactId>.jar` with the version stripped (`kotlin-compiler.jar`, `kotlin-daemon.jar`,
  `kotlinx-coroutines-core-jvm.jar`; `annotations-13.0.jar` is a legacy exception matching the
  kotlinc dist's own naming). Unversioned names are exactly what
  `KotlinPathsFromBaseDirectory.jar()` (`KotlinPaths.kt:153`) needs.
- The closure really is transitive, not a curated list: `kotlin-build-tools-api.jar` is already in
  that directory although `kotlin-dist-for-jps-meta/pom.xml` never names it — it arrives through
  `kotlin-compiler`'s pom.

So one dependency line brings the whole runtime set, because `kotlin-build-tools-impl`'s pom
declares the rest as `runtime`:

| artifact | how it arrives | size |
|---|---|---|
| `kotlin-build-tools-impl` | added to `kotlin-dist-for-jps-meta/pom.xml` | 18 MB |
| `kotlin-compiler-embeddable` | transitive, `runtime` | 181 MB |
| `kotlin-build-tools-cri-impl` | transitive, `runtime` | 2 MB |
| `kotlin-tooling-core` | transitive, `runtime` | 1 MB |
| `kotlin-compiler-runner` | transitive, `runtime` | 1 MB |

Required work, then:

- Add `kotlin-build-tools-impl` to `libraries/tools/kotlin-dist-for-jps-meta/pom.xml`.
- Add a `KotlinPaths.Jar` constant and a `ClassPaths.BuildToolsApi` entry in
  `compiler/util/src/org/jetbrains/kotlin/utils/KotlinPaths.kt:117-128`, alongside the existing
  `CompilerWithScripting`.
- `JpsBtaToolchainsProvider` then resolves each `ClassPaths.BuildToolsApi` entry through
  `kotlinPaths.jar(...)`. Entries that do not exist are dropped rather than handed to the
  classloader, since the exact set a distribution ships varies, but they are **reported**: the two
  layouts are assembled by different builds and can disagree, and a silently dropped jar surfaces
  much later as a `NoClassDefFoundError` from inside the isolated classloader. Only
  `kotlin-build-tools-impl` is checked hard.

`dist/kotlinc/lib` still matters, but only for the repo's own suites: `-Dkotlin.jps.tests=true`
makes `computeKotlinPathsForJpsPlugin` (`KotlinBuilder.kt:626-643`) resolve to
`PathUtil.kotlinPathsForDistDirectory`, which held 58 jars and none of the build-tools ones. The
same jars therefore have to be added to `distLibraryProjects` (`prepare/compiler/build.gradle.kts:82`),
where dependencies are resolved with `isTransitive = false` and so must be listed one by one.

**The embeddable is not optional — settled empirically.** The hope was that the impl could run
against the `kotlin-compiler.jar` already present in both layouts, saving 181 MB. It cannot:
`dist/kotlinc/lib/kotlin-compiler.jar` ships `com/intellij/openapi/util/Disposer.class` *unrelocated*,
while `kotlin-build-tools-impl` is compiled against the embeddable's shaded
`org.jetbrains.kotlin.com.intellij.*` packages. Running against the plain compiler jar fails on the
very first operation:

```
java.lang.NoClassDefFoundError: org/jetbrains/kotlin/com/intellij/openapi/util/Disposer
    at org.jetbrains.kotlin.buildtools.internal.ApplicationEnvironmentPin.<init>
```

So `ClassPaths.BuildToolsApi` is built around `Jar.CompilerEmbeddable` and deliberately excludes
`Jar.Compiler` — having both on one classloader would mean two copies of the compiler under
different package names.

One entry cannot be expressed as a `KotlinPaths.Jar` at all: the JetBrains annotations jar carries
its version in the file name (`annotations-13.0.jar`) in both layouts, so `JpsBtaToolchainsProvider`
picks it up by prefix from `libPath`. It is not optional either — without it code generation fails
with `NoClassDefFoundError: org/jetbrains/annotations/NotNull` the moment a function returns a
non-null type.

Caching mirrors what the legacy path already does:

- `KotlinToolchains` (and its `URLClassLoader`) in a static `SoftReference` keyed by impl
  classpath, exactly like `CompilerRunnerUtil.ourClassLoaderRef`
  (`CompilerRunnerUtil.kt:32,57-70`). Without this, every build reconstructs a compiler classloader.
- The `BuildSession` — which is `AutoCloseable` and owns caches — created lazily on first Kotlin
  chunk and closed in `KotlinBuilder.buildFinished` (`KotlinBuilder.kt:166`), held in a
  `GlobalContextKey` next to the existing `KotlinCompileContext` key.

The two are held **together**, as a single `JpsBtaBuild`, and every chunk reads the toolchains from
it rather than from the provider. Because the provider's reference is soft, a second
`KotlinToolchains` on a second classloader can appear part-way through a build — most likely under
exactly the memory pressure that compiling causes. Building an operation from those and executing
it on the older session mixes two compiler classloaders in one call.

Execution policy is `kotlinToolchains.createInProcessExecutionPolicy()`. Note that
`JvmCompilationOperationImpl.compileInProcess` calls `setupIdeaStandaloneExecution()`, which sets
JVM-global system properties (`compiler/cli/cli-base/.../compat.kt:17-35`). This is not a new risk:
today's in-process fallback reaches the same code through `K2JVMCompiler.exec`.

### Result mapping

`CompilationResult` → the `Boolean` that `compileModuleChunk` returns. Compile errors already reach
JPS through the message bridge, so `KotlinBuilder` fails the build on its own. The one gap to close
explicitly: `COMPILER_INTERNAL_ERROR` and `COMPILATION_OOM_ERROR` can arrive without any reported
`ERROR` message, so the runner reports one itself before returning, otherwise the build would
silently pass.

## Files

**New** — `jps/jps-plugin/src/org/jetbrains/kotlin/compilerRunner/btapi/`: the six classes above.

**Modified**
- `jps/jps-plugin/src/org/jetbrains/kotlin/jps/targets/KotlinJvmModuleBuildTarget.kt` — flag branch
  in `compileModuleChunk`, the three guards, the `hasCaches` override, and assembling the
  incremental run
- `jps/jps-plugin/src/org/jetbrains/kotlin/jps/targets/KotlinModuleBuildTarget.kt` —
  `isIncrementalCompilationDelegatedToCompiler`
- `jps/jps-plugin/src/org/jetbrains/kotlin/jps/build/KotlinBuilder.kt` — session lifecycle in
  `buildFinished`, and skipping JPS's own incremental analysis
- `jps/jps-plugin/src/org/jetbrains/kotlin/compilerRunner/JpsKotlinCompilerRunner.kt` — extract the
  argument-flattening helper
- `compiler/util/src/org/jetbrains/kotlin/utils/KotlinPaths.kt` — new `Jar` + `ClassPaths` entries
- `compiler/util/src/org/jetbrains/kotlin/utils/PathUtil.kt` — jar base names for those entries
- `libraries/tools/kotlin-dist-for-jps-meta/pom.xml` — impl jar into the IDE-facing JPS dist
- `prepare/compiler/build.gradle.kts` — impl, cri-impl, embeddable, compiler-runner and
  tooling-core into `dist/`,
  for the repo's own test suites

No change is needed in `jps/jps-plugin/build.gradle.kts`: `kotlin-build-tools-api` arrives through
`CompilerModules.kotlinJpsPluginMavenDependencies`, which is already wired as `implementation`.

Reused as-is: `MessageCollectorAdapter`, `OutputItemsCollectorImpl`,
`OutputMessageUtil.parseOutputMessage`, `JpsKotlinLogger`, `collectSourcesToCompile`,
`findClassPathRoots`, `findModularJdkRoot`, `friendOutputDirs`, `registerOutputItems`,
`getGeneratedFiles`.

## Verification

The primary loop is the playground, not the test suites.

```bash
# JAVA_HOME must point at JDK 11+ here — the spdx-maven-plugin breaks under the shell default JDK 8
./gradlew installJps
```

Then, in the playground:

1. Delete the materialised dist for this version, because IntelliJ keys that cache by version alone
   and will not refresh it for a rebuilt `-SNAPSHOT`:
   ```bash
   rm -rf ~/Library/Caches/JetBrains/IntelliJIdea2026.2/kotlin-dist-for-ide/2.5.255-SNAPSHOT
   ```
   Then reopen the project so it is materialised again, and confirm the jars actually landed:
   ```bash
   ls ~/Library/Caches/JetBrains/IntelliJIdea2026.2/kotlin-dist-for-ide/2.5.255-SNAPSHOT/lib | grep build-tools
   ```
   Expect `kotlin-build-tools-impl.jar` next to the `kotlin-build-tools-api.jar` that is there
   already, plus `kotlin-compiler-embeddable.jar`, `kotlin-compiler-runner.jar` and
   `kotlin-tooling-core.jar`. If they are missing, nothing downstream can work — stop and fix
   packaging first. `JpsBtaToolchainsProvider` also warns about whatever it had to drop, so the
   build log answers this question too.
2. Add `-Dkotlin.jps.useBuildToolsApi=true` to *Shared build process VM options*.
3. Build Project. Incremental compilation may be left on, but only modules without module
   dependencies will build; with it off, any module builds.

What to assert beyond "it compiled", since these are the things a passing compile can still get
wrong:

1. **Output registration** — delete a source file and rebuild; its class file must disappear from
   `out/production/...`. This only works if `registerOutputFile` received correct source→output
   pairs, i.e. it is the real test of the `Severity.OUTPUT` bridge.
2. **Diagnostics** — an intentional compile error must surface in the Build tool window with the
   correct file path, line and column, be clickable, and fail the build.
3. **Cyclic chunks** — make `util` depend back on `jps-playground` and confirm the explicit error,
   rather than a miscompile.
4. **Classloader isolation** — confirm the compiler is loaded through
   `KotlinToolchains.loadImplementation` and not from the JPS plugin classloader; a debug log of
   `kotlinToolchains.getCompilerVersion()` at session creation is the cheap check.

In the repo, the same four assertions are covered by `BuildToolsApiKotlinJpsBuildTest`, along with
incrementality (a changed file recompiles, an unrelated one does not, a dependent one does, and
*Rebuild* recompiles everything). It is the
faster feedback loop and does not need `installJps` at all — it resolves `jps.kotlin.home` to
`dist/kotlinc` through `-Dkotlin.jps.tests=true`:

```bash
./gradlew :jps:jps-plugin:test --tests "org.jetbrains.kotlin.jps.build.BuildToolsApiKotlinJpsBuildTest" -q
```

The flag is set from inside the test rather than on the command line: nothing forwards `-D` from the
Gradle invocation to the test JVM, and the property that switches incremental compilation is
`kotlin.incremental.compilation` (`IncrementalCompilation.INCREMENTAL_COMPILATION_JVM_PROPERTY`),
not `kotlin.incremental.jvm`.

The incremental tests assert on class file timestamps rather than on the runner's own progress line.
One build can run several compilations: JPS's Java dependency graph may mark more files dirty after a
round, and each round is a separate operation. `assertCompiled` is no help either — it observes what
JPS marked dirty, not what the compiler chose to recompile.

The legacy path must still pass with the flag off — that is the regression check on the extracted
argument helper and on the `hasCaches` override. `KotlinJpsBuildTestIncremental` is the important one
of these, since it exercises everything the incremental work touches with the BTA path off:

```bash
./gradlew :jps:jps-plugin:test --tests "org.jetbrains.kotlin.jps.build.KotlinJpsBuildTest" \
    --tests "org.jetbrains.kotlin.jps.build.KotlinJpsBuildTestIncremental" \
    --tests "org.jetbrains.kotlin.jps.build.SimpleKotlinJpsBuildTest" \
    --tests "org.jetbrains.kotlin.jps.build.JpsKotlinCompilerRunnerTest" -q
```

## Diagnosing a build

JPS has no `--info`/`--debug`, and the *Build* tool window shows only what a builder reports as a
`BuildMessage`. The knobs below are what a consumer has, roughly in the order they are reached for.
All the `-D` ones go into *Settings | Build, Execution, Deployment | Compiler | Shared build process
VM options*, next to `-Dkotlin.jps.useBuildToolsApi=true`.

**`-Dkotlin.jps.verbose=true`** — the switch this path adds, and the closest thing to `--info`
combined with `--debug`. It promotes into the *Build* tool window everything that would otherwise be
written to the build process log:

- the compile set, the classpath and the flattened argument strings of each module
  (`JpsBuildToolsApiCompilerRunner.logCompilationInput`), and the sources the compiler actually
  recompiled;
- the incremental compilation reports of the Build Tools API implementation, which are emitted only
  when the `KotlinLogger` it was handed says `isDebugEnabled`;
- compiler diagnostics that arrive at the renderer as `Severity.DEBUG`, which `MessageCollectorAdapter`
  would otherwise drop into the log.

`JpsBtaLogger` is the mechanism for the first two, `JpsCompilerMessageRendererBridge` for the third.
The switch affects the Build Tools API path only; it does not touch the legacy runner. The three
`[Build Tools API]` progress lines are reported regardless of it.

**The build process log** — the build runs in its own JVM, whose log is `build.log` under
`<IDE log dir>/build-log/` (`~/Library/Logs/JetBrains/<product>/build-log/` on macOS). Its levels come
from `build-log-jul.properties` in that same directory and nowhere else: `LogSetup.initLoggers` in
`jps-build` reads it at process start, seeding it from `defaultLogConfig.properties` the first time.
It is `java.util.logging` syntax, a category is a logger name, and the leading `#` of an IntelliJ
logger name has to be escaped or the line is a properties comment:

```properties
\#org.jetbrains.jps.level=FINER                # JPS itself: build order, dirty sets, dependency graph
\#org.jetbrains.kotlin.jps.level=FINER         # every Kotlin JPS category, this path included
```

`FINER` is what `Logger.debug` maps to. The file is read when the build-process JVM starts and the
IDE keeps a preloaded one, so a change in Compiler settings or an IDE restart is needed before edits
take effect.

**Compiler-side verbosity** — `-verbose` or `-Xprint-configuration` under *Settings | Build,
Execution, Deployment | Compiler | Kotlin Compiler | Additional command line parameters*.
`AbstractConfigurationPhase` then dumps the whole `CompilerConfiguration` through `reportInfo`, which
reaches the *Build* tool window through the renderer bridge. Deliberately not folded into
`kotlin.jps.verbose`: compiler arguments come from the facet and the project settings, and this path
round-trips them through argument strings precisely so that it never has to inject any of its own.

**`-Djps.report.build.statistics=true`** — makes `BuildSession` promote its per-builder
`Build duration: Builder 'Kotlin Builder' took ...; N sources processed` line from the log to a
`JPS_INFO` build message. The cheap way to A/B this path against the legacy one.

**`-Dkotlin.build.report.file.output_dir=<dir>`** — makes `JpsStatisticsReportService.create()` return
the real implementation instead of `DummyJpsStatisticsReportService` and write a readable per-build
report of timings and changed files. `kotlin.build.report.file.change_file_limit` caps the file list;
an HTTP sink exists too (`kotlin.build.report.http.url` and friends).

**Attaching a debugger** — `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`,
then a Remote JVM Debug configuration; the `compiler.process.debug.port` registry key does the same.
The build process is shared across projects and preloaded, so it is worth confirming which one was
attached to.

## Out of scope, and what each will need

Stated explicitly so none of these is mistaken for an oversight:

- **Incremental compilation across modules.** Classpath snapshotting, which is what would let a
  module notice that one of its dependencies changed. See
  [Incremental compilation](#incremental-compilation) for what is implemented instead.
- **JPS's Kotlin↔Java dependency tracking under incremental compilation.** JPS's caches and lookup
  storage stay empty while the compiler owns incrementality, so `updateCaches`,
  `updateLookupStorage` and `processChangesUsingLookups` contribute nothing. `updateChunkMappings`
  still runs and still feeds JPS's Java dependency graph from the generated class files, but the
  `InlineConstTracker` / `EnumWhenTracker` / `ImportTracker` it reads are empty on this path.
- **Mixed Java/Kotlin modules.** Not an argument problem — `-Xjava-source-roots` and
  `-Xjava-package-prefix` both exist and flow through `applyArgumentStrings`, and BTA's `sources`
  list accepts `.java` paths. What is out of scope is the interaction with JPS's `JavaBuilder`:
  chunk ordering, `updateChunkMappings` feeding the dependency graph, and the per-root
  `packagePrefix` gap noted under [Argument mapping](#argument-mapping).
- **Cyclic module chunks.** Needs a multi-module compilation operation in BTA — one operation with
  several (sources, output dir, friend dirs) groups. This is the one genuine BTA API gap this work
  uncovers.
- **Daemon execution policy.** `ExecutionPolicy.WithDaemon` exists; wiring it means deciding what
  happens to the JPS daemon session cache (`JpsKotlinCompilerRunner._jpsCompileServiceSession`).
- **Bytecode instrumentation.** `registerOutputItems` reads class bytes back for
  `registerCompiledClass` when `-Dkotlin.jps.instrument.bytecode=true`; unaffected by this change
  but untested on the BTA path.
- **A structured diagnostic/output listener in BTA**, which would retire the renderer-as-bridge
  workaround and recover the `EXCEPTION` vs `ERROR` distinction.
- **Shipping this for real.** As a spike, nothing here is gated on artifact size, `artifacts-tests`
  resources, or the `kotlinJpsPluginMavenDependencies` list. A mergeable version would have to argue
  for the 181 MB the embeddable adds to every IDE's JPS dist download.
