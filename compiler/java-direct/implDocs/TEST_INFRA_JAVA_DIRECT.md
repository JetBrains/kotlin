# java-direct in the shared test infrastructure

**Status**: the source-root gap is fixed (2026-08-13), and so is blocker (a), the `SUPER_TYPES`
lazy-resolve contract violation. One blocker remains before a facade-based suite can be run under
`-Xjava-direct` — the `ForeignAnnotations` golden data, §3b.

Since the `FirJavaInterop` rounds, `FirFrontendFacade` (`compiler/tests-common-new`) derives the
Java view from `projectEnvironment.javaInterop(configuration)`, i.e. the whole non-CLI JVM test
infrastructure *would* honour `-Xjava-direct`. Nothing sets the flag there yet, so the capability
was latent and untested. This note records the experiment that exercised it.

## 1. The experiment

`javaInterop` was temporarily forced to the java-direct branch unconditionally
(`if (true || configuration.useJavaDirect)`) and the JVM-hosted suites were run. Baseline with the
same tree and the flag off: `analysis-tests` 56672/0, `fir2ir` 73656/0 — so every difference below
is caused by java-direct.

| Suite | Forced on | Verdict |
|---|---|---|
| `fir:analysis-tests` | 109 failures / 56672 | see §3; **19** after (a) was fixed |
| `fir:fir2ir` (codegen + IR text, incl. multi-module) | 3 failures / 73656 | all "test can be unmuted": java-direct is *more* capable than the muted expectation |
| `JvmLightTreeBlackBoxCodegenWithSeparateKmpCompilation` (HMPP) | 269/0 | green |
| `jklib.tests` | 843/0 | green |
| `IncrementalK2FirICJvmCompilerRunnerTest` | 371/0 | green |
| `scripting-tests` | green | `withJavaSources = false` there |

So the interop wiring of every JVM-hosted pipeline — JKlib, incremental compilation, the HMPP
common-fragment classpath, scripting/REPL — works with java-direct; the failures are all in one
kind of consumer.

## 2. The source-root/`Context` mismatch — real, was unexercised, fixed

`FirFrontendFacade` builds one `FirJvmSessionFactory.Context` from the **leaf** module's
`CompilerConfiguration` for the whole `dependsOn` closure (`sortDependsOnTopologically`), while
`JvmEnvironmentConfigurator` registered `.java` source roots for *that module only* — and returned
early when it had none. The PSI peer does not care, because `AllJavaSourcesInProjectScope` is "any
`.java` file in the project"; java-direct takes an explicit `List<JavaSourceRootEntry>` and would
have seen a subset.

A temporary assertion in `FirFrontendFacade.analyze`, comparing the Java source roots of every
module of the closure against the configuration actually used, found **zero** hits over
`analysis-tests` + `fir2ir` (130k tests): no `dependsOn` module in the corpus declares a `.java`
source root — the HMPP Java data keeps Java in the leaf platform modules. Ordinary (non-`dependsOn`)
multi-module tests are unaffected, because each module gets its own configuration and its own
`analyze()` call.

Fixed in `JvmEnvironmentConfigurator.configureCompilerConfiguration`: the Java source roots of the
whole `dependsOn` closure are registered, mirroring `addSourcesForDependsOnClosure`, which already
does exactly this for the *Kotlin* sources of the same configuration. For a non-MPP module the
closure is `[module]`, so the change is provably behaviour-neutral for the current corpus — and it
is the test infrastructure's asymmetry only: the real CLI takes its Java source roots from
arguments, so there is nothing to fix in production.

**Not covered**: no test data exercises the fixed path. Adding an HMPP fragment with `.java` files
is the natural regression test, but it needs new test data plus a generated runner, and "a `.java`
file in a non-leaf fragment" is itself a modelling question — left out deliberately.

## 3. What forcing java-direct on actually breaks

Both are pre-existing java-direct defects that the CLI phased suite does not reach; neither is
related to §2.

**(a) `FirLazyResolveContractViolationException` at `SUPER_TYPES` — 90 failures — FIXED
(2026-08-13)**, all in
`FirLightTreeDiagnosticsWithLatestLanguageVersionTestGenerated` (J+K test data). The site is arm 3
of `directSupertypeClassIds` (`resolution/JavaTypeResolver.kt:534`),
`symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)` — i.e. Java-source resolution asking a
**Kotlin** source class for its supertypes. That runner installs the checked lazy resolver, which is
why the same data passes in `AbstractJavaUsingAstTest` (CLI phased pipeline): the violation is
latent everywhere, not specific to the facade.

The answer to "what does a Java source file see of a Kotlin class whose supertypes are not resolved
yet" is now the same one the PSI peer `FirJavaElementFinder` gives: resolve them **on air**. Arm 3
reads `supertypeRefsForJavaResolution` (shared with the model-side `FirBackedJavaClassAdapter`,
which already did exactly this) and the `lazyResolveToPhase` call — a no-op in the compiler, and an
unsanctioned intra-phase jump — is gone. Silently answering "no supertypes" was not harmless: it
turns an inherited nested class reached through a Kotlin link into an unresolved reference, and makes
a `protected` nested class look inaccessible. Both are pinned by new test data,
`{,protected}InheritedNestedClassThroughKotlinSupertype.kt` in
`testData/diagnostics/tests/jvm/javaDirect/`, which failed before the change and pass after it (the
PSI runner passes them with the same expected output). Re-measured with java-direct forced on:
`fir:analysis-tests` 19 failures / 56676, i.e. all 90 gone, only (b) left.

**(b) `ForeignAnnotations` golden data — 19 failures** in the PSI-parser suites
(`*ForeignAnnotationsCompiledJava*`, `*ForeignAnnotationsSourceJava*`): output divergences, not
crashes. The repo already has the mechanism for this — `ForeignAnnotationAgainstCompiledJavaTestSuppressor`
and `PsiClassFilesReadingForCompiledJavaTestSuppressor` — so these need a java-direct suppressor or
per-test suppression. These 19 are the only failures left with java-direct forced on.

## 4. Remaining work to make it usable

1. Decide (b)'s suppressor. ((a) is fixed.)
2. Promote `JavaDirectConfigurator` (today private to `:compiler:java-direct` testFixtures) into
   `tests-common-new` with a directive, so the flag is per suite rather than global; generate
   java-direct variants of the interesting facade-based runners, as `ForeignAnnotations` and the
   light-tree/PSI pairs already do.
3. `AbstractFirTypeEnhancementTest` stays on PSI by declaration: its Java files are in-memory
   `LightVirtualFile`s (`PsiFileFactoryImpl.trySetupPsiForFile`), so there is no directory for
   java-direct to read.
4. Re-confirm that `registerKotlinDeclarationsForJava` being a no-op under java-direct is safe for
   facade-based suites; the existing evidence was collected on the phased/CLI path only.
