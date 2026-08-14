# java-direct in the shared test infrastructure

**Status**: done (2026-08-14). java-direct is the **default** Java view of the whole JVM test
infrastructure, and both blockers are gone: (a) the `SUPER_TYPES` lazy-resolve contract violation and
(b) the `ForeignAnnotations` divergences, which turned out not to be golden-data noise at all (§3b).
Measured on the default configuration: `fir:analysis-tests` 56831/0,
`fir:analysis-tests:legacy-fir-tests` 292/0, `fir:fir2ir` 73778/0, `compiler:java-direct` 0 failures.

Since the `FirJavaInterop` rounds, `FirFrontendFacade` (`compiler/tests-common-new`) derives the
Java view from `projectEnvironment.javaInterop(configuration)`, i.e. the whole non-CLI JVM test
infrastructure honours the same switch as the CLI. This note records the experiment that first
exercised it and what had to be fixed to make it the default.

## 1. The experiment, and how the flag reads now

`javaInterop` was temporarily forced to the java-direct branch unconditionally
(`if (true || configuration.useJavaDirect)`) and the JVM-hosted suites were run. Baseline with the
same tree and the flag off: `analysis-tests` 56672/0, `fir2ir` 73656/0 — so every difference below
is caused by java-direct.

That scaffolding is gone. `javaInterop` now reads `JVMConfigurationKeys.USE_JAVA_DIRECT` with
`?: true`, because the key is *unset* in every pipeline which does not build its configuration from
CLI arguments — the whole non-CLI test infrastructure, the Build Tools API, scripting — and "unset"
must mean the same as the `-Xjava-direct` default, i.e. on. A consumer whose subject *is* the PSI
view opts out explicitly; today there is exactly one, §3b.

| Suite | Forced on | Verdict |
|---|---|---|
| `fir:analysis-tests` | 109 failures / 56672 | see §3; **19** after (a) was fixed, **0** after (b) |
| `fir:fir2ir` (codegen + IR text, incl. multi-module) | 3 failures / 73656 | all "test can be unmuted": java-direct is *more* capable than the muted expectation; mutes removed, §3c |
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

## 3. What turning java-direct on actually broke

All four are pre-existing java-direct defects (or stale expectations) that the CLI phased suite does
not reach; none is related to §2. All four are fixed — nothing here is suppressed.

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

**(b) `ForeignAnnotations` — 19 failures — FIXED (2026-08-14)**, and *not* by a suppressor. The
earlier reading of these as "golden data divergences needing per-test suppression" was wrong; they
were two unrelated things.

**16 of them are external annotations**, and those are a PSI-only facility by construction: they
reach the Java model through `ExternalAnnotationsManager` over a `PsiModifierListOwner`. java-direct
is PSI-free by design and has no `annotations.xml` peer — and neither does the compiler, which
registers a `MockExternalAnnotationsManager` that finds nothing. So a module declaring external
annotations is *asking for* the PSI view, and `ExternalAnnotationsEnvironmentConfigurator` now says
so (`configuration.useJavaDirect = false` when the module has an `annotations.xml`). This is the one
sanctioned opt-out; it is a property of the facility under test, not a suppression of a defect.

**The other 3 were a real java-direct defect** (`java8Tests/misc/{returnType,valueParameter}`,
`jspecify/strictMode/unstableEnhancedNullability`): an annotation written *before* an array type
annotates the **element** type (JLS 9.7.4 — `@NotNull R []` vs `R @NotNull []`), and it was dropped,
so `Array<@NotNull R & Any>` came out as `Array<R!>` and JSpecify's `@NonNull` never reached the loop
variable. Fixed in `model/JavaTypeOverAst.kt` together with §3d; pinned by
`JavaParsingAnnotationsTest.testArrayElementAndLevelAnnotations`.

**(c) `fir2ir` — 3 stale mutes — FIXED (2026-08-14)**. All three were `BlackBoxCodegenSuppressor`'s
"looks like this test can be unmuted", i.e. java-direct is more capable than the expectation:
`// IGNORE_HEADER_MODE: JVM_IR` removed from the three
`testData/codegen/boxJvm/javaFieldAndKotlinProperty/java{Field,Protected}*.kt` files. The
non-header-mode `IGNORE_BACKEND: JVM_IR` mute (KT-56386) is untouched and still in force.

**(d) `AbstractFirTypeEnhancementTest` — 17 failures — FIXED (2026-08-14)**. This suite was
previously believed unable to use java-direct at all ("its Java files are in-memory
`LightVirtualFile`s, so there is no directory for java-direct to read") — that was wrong. It writes
every test file to a real `javaFilesDir` and passes it to `newConfiguration` as a Java source root;
the `LightVirtualFile`s are only a second, in-memory mirror used to enumerate the `PsiClass`es whose
`ClassId`s the test then asks the symbol provider for. What hid the directory was
`FirTestSessionFactoryHelper.createSessionForTests`, which fabricated an **empty**
`CompilerConfiguration` just to carry `languageVersionSettings` and handed *that* to `javaInterop`.
The PSI peer does not notice — `AllJavaSourcesInProjectScope` ignores the configuration — while
java-direct correctly reported "no Java sources". The helper now takes the configuration of the
compilation under test.

With the Java files visible, the suite became a dense differential test of the model against the PSI
peer and exposed eight genuine java-direct defects, all fixed in `compiler/java-direct/src`:
annotation arguments that are constant *expressions* rather than literals (routed through the real
`ConstantEvaluator` instead of a private mini-evaluator); `TYPE_CAST_EXPRESSION` constants, i.e.
`byte`/`short` `static final` initializers; enum entries emitted after the other members instead of
first; a Java enum constructor reported package-private instead of implicitly `private` (JLS 8.9.2);
implicit outer type arguments of an *inherited* non-static inner class rendered as unresolved names;
recovered outer arguments losing their flexibility; TYPE_USE annotations propagated onto every level
of a multi-dimensional array instead of the level they are written at; and a raw-type walk that
ignored `static` enclosing classes.

## 4. Pinning the constant-evaluator fixes in the box corpus

The two constant-evaluator defects of §3d were invisible to the java-direct suites for two different
reasons: the only data exercising them lives in `testData/loadJava`, which those suites do not model,
and their runners assert diagnostics and box output, never the Java model. The first half is now
covered without touching the model list, because `codegen/box(Jvm)/evaluate/*` is the one place in
the box corpus where the frontend *is* observable: `commonCodegenConfiguration`
(`tests-common-new/.../configuration/CommonCodegenConfiguration.kt`) enables `FIR_DUMP` with
`RENDER_SPECIFIC_FIR_DECLARATION_ATTRIBUTES = EvaluatedValue` for every test under that directory, so
the evaluated value of a Java constant reaches a golden `.fir.txt`.

`testData/codegen/boxJvm/evaluate/constEvaluationFromJavaWorld/javaConstantExpressions.kt` uses that.
It declares Java `static final` fields in every non-literal form — narrowing cast (`(byte) 300`,
`(short) 70000`, `(char) 65`, `(byte) (LOCAL * 100)`, `(int) 2.75`, `(char) ('A' + 1)`), shifts and
bitwise ops, parenthesized and unary expressions, a relational and a boolean operator, references to
another constant of the same class, of another class and through a `static` import, and polyadic
concatenation — and consumes each of them from a Kotlin `const val` and from an annotation argument.
That makes each value observable three times: as a compile-time requirement (a lost constant is
`CONST_VAL_WITH_NON_CONST_INITIALIZER`), as `[EvaluatedValue=…]` / `[evaluated = …]` in the golden
dump, and at runtime, where `box()` compares every value — including the annotation's, read back by
reflection — against the JLS result.

Verified to be revert-sensitive: dropping either half of the cast fix — the
`TYPE_CAST_EXPRESSION` arm of `ConstantEvaluator.evaluate` or the one of
`JavaFieldOverAst.isInitializerPotentiallyConstant` — fails the test. It passes in
`JavaUsingAstBoxTestGenerated` and, on the same golden file, in the `FirLightTree`/`FirPsi`
black-box and header-mode codegen runners.

**What a box test still cannot see**: the value of an argument of a *Java* annotation, i.e. the other
half of §3d. `FirDumpHandler` renders only the Kotlin files of the compilation, and
`FirScopeDumpHandler` renders members with `FirRenderer.noAnnotationBodiesAccessorAndArguments()`, so
no box or diagnostics assertion depends on `@Anno("hello")` — the frontend itself only consumes
*enum* and class-literal arguments of Java annotations. Those defects are observable exclusively in a
rendered Java model (`loadJava`) or in the module's own unit tests
(`JavaConstantEvaluatorTest`, `JavaParsingAnnotationsTest`); more box/diagnostics corpus would not
help.

## 5. Where each defect is pinned

Every defect of §3 now has a test in a corpus which does not depend on java-direct being on by
default, and each was checked to fail with its fix reverted. "Kotlin-visible" means a runner that
consumes the Java model through the frontend (both java-direct's own suites and the PSI-parser
ones); "model" means a unit test of `:compiler:java-direct` over the light tree.

| Defect | Test | Kind |
|---|---|---|
| constant *expressions* as `static final` initializers, incl. `(byte)`/`(short)` casts | `codegen/boxJvm/evaluate/constEvaluationFromJavaWorld/javaConstantExpressions.kt` | Kotlin-visible (`const val` + `FIR_DUMP` + runtime) |
| the same, at the level of the evaluator | `JavaConstantEvaluatorTest.testCastInConstantInitializer` | model |
| constant expressions as annotation *arguments* and as annotation-method *defaults* | `JavaParsingAnnotationsTest.testConstantExpressionAnnotation{Arguments,MethodDefault}` | model (see §4: no diagnostic depends on such a value) |
| enum entries before the other members; enum constructor implicitly `private` | `JavaParsingModifiersAndSpecialClassesTest.testEnum{ConstantsComeBeforeOtherFields,ConstructorIsImplicitlyPrivate}` | model (neither is observable from Kotlin) |
| outer type arguments of an *inherited* inner class | `diagnostics/tests/j+k/inheritedInnerOuterArgs.kt` | Kotlin-visible (argument loss ⇒ type mismatch) |
| `static`-aware raw-type walk | `diagnostics/tests/j+k/staticNestedRawType.kt` | Kotlin-visible |
| TYPE_USE annotation bound per array level and to the element type | `diagnostics/tests/j+k/javaArrayTypeUseAnnotationLevels.kt`, `JavaParsingAnnotationsTest.testArray{ElementAndLevelAnnotations,LevelAnnotationsOnField}` | Kotlin-visible (rendered type per level) + model |
| annotation in front of a *qualified* name binds to its first segment | `loadJava/compiledJava/typeUseAnnotations/Basic` (`f7`/`f81`/`f9`) | golden Java-model dump |

Two lessons are worth keeping. First, the three model-only rows are not laziness: no diagnostic and
no bytecode depends on the order of enum entries, on the visibility of a constructor which cannot be
called, or on the value of a Java annotation's string argument — the frontend consumes only *enum*
and class-literal arguments of Java annotations. Second, the Kotlin-visible rows had to be written so
that a wrong model is a *hard* failure (a type mismatch, a lost `const`) wherever possible, and a
rendering difference only where nothing else is available: `DEBUG_INFO_EXPRESSION_TYPE` is the only
assertion in the diagnostics corpus that shows type annotations at all.

Two of the diagnostics tests had to be written against the *rendered type* for a less obvious
reason, worth recording because it will come up again: both defects produce a **lenient** type. An
unresolved type argument becomes an error type and a raw type is flexible, and either is silently
compatible with anything — the first drafts of `inheritedInnerOuterArgs.kt` and
`staticNestedRawType.kt` passed with their fix reverted precisely because "call it and assign the
result" proves nothing there. For the same reason the raw-type test must nest a `static` class
*between* the generic outer and the referenced inner one: `computeIsRaw` walks the outer chain only
for a non-static classifier, so a plain `Outer.Nested<String>` never reaches the code the fix
changed.

## 6. Remaining work

1. The PSI peer is now reachable only through `-Xjava-direct=false` or an explicit
   `useJavaDirect = false`. Nothing generates a *PSI* variant of a facade-based runner any more, so
   the peer's own coverage is whatever the external-annotation suites give it. Decide whether that is
   enough, or whether the light-tree/PSI runner pairs should gain a java-direct/PSI axis instead
   (`JavaDirectConfigurator` is still private to `:compiler:java-direct` testFixtures and would have
   to move into `tests-common-new` with a directive).
2. Re-confirm that `registerKotlinDeclarationsForJava` being a no-op under java-direct is safe for
   facade-based suites; the existing evidence was collected on the phased/CLI path only.
3. External annotations (§3b) remain unimplemented in java-direct — deliberately, as they are an IDE
   facility the compiler does not honour either. If the Analysis API ever needs them over java-direct,
   that is a new feature, not a regression of this work.
