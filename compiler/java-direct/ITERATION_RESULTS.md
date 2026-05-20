# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%).

**Last Updated**: 2026-05-12 (live log reset; all entries 2026-04-22 → 2026-05-11
archived to `implDocs/archive/ITERATION_RESULTS_2026_05_11.md`).

> **Caveat on historical numbers.** Before 2026-04-28, the `JavaUsingAst*` test
> generators did **not** actually route `// FILE: *.java` blocks through
> `java-direct`'s AST — they fell through to PSI's `JavaClassFinderImpl`. Any
> "1168/1168 box" / "1454/1456 phased" / "feature complete" status claim dated
> before 2026-04-28 was measured against the PSI loader, not `java-direct`. The
> 2026-04-28 framework fix grew the suite to 2793 tests and surfaced fresh
> regression categories, all resolved by 2026-05-11.

## Recent history (one-liners)

- **2026-05-20** — Lombok-plugin compatibility with `java-direct`. Two fixes:
  1. **`JavaImportResolver.extractFragmentedImports`** — recover single-segment
     star imports (`import lombok.*;`) when the parser fragments the file root
     because of leading `// FILE: …` comments (Lombok testdata pattern). Previous
     guard required a dot in the recovered FQN and silently dropped `lombok`.
  2. **`JavaField.hasInitializer`** added to the public interface (rule §7
     exception — see [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md)). Required
     because the Lombok K2 generators (`AllArgsConstructorGeneratorPart`,
     `RequiredArgsConstructorGeneratorPart`) previously cast `source.psi as PsiField`
     to call `hasInitializer()`, returning `false` for any non-PSI Java-model
     impl (including `JavaClassOverAst`). The cast was itself a PSI leak in the
     K2 plugin — net debt **reduction**, not an addition: PSI is removed from the
     plugin call path. Subinterface-on-`compiler/java-direct/` doesn't fit because
     PSI's `JavaFieldImpl` also has to expose this so PSI-loaded fields keep
     their non-constant-initializer detection. Implementations: PSI
     `JavaFieldImpl`, java-direct `JavaFieldOverAst`, ASM `BinaryJavaField`,
     `javac-wrapper` `TreeBasedField`/`SymbolBasedField`/`MockKotlinField`,
     reflection `ReflectJavaField`. `FirJavaField` gains
     `lazyHasInitializer` / `hasInitializer`; `FirJavaFacade` populates it;
     `SignatureEnhancement` propagates it on copy.

- **2026-05-11** — Cat E ASM `Frame.merge` crashes resolved: traced to
  `JavaFieldOverAst.initializerValue` not coercing the evaluated constant to
  the field's declared primitive type. All 11 java-direct-only IJ FP failures
  now pass.
- **2026-05-08 → 2026-05-10** — IJ FP regression delta cleanup (Cat A-E):
  inherited-nested-class lookup over binary supertypes, private interface
  methods, Scala companion-module `$` filter, qualified raw-form nested
  classes, cross-language `ConstantEvaluator`, star-imported binary
  supertypes, `@NotNull T[]` double application, and nested-class
  explicit-import `ClassId` splitting.
- **2026-05-08** — `LazySessionAccess` re-entrance guard (KT-74097 / same-thread
  `PUBLICATION` lazy re-entrance), `extractStaticImports` parser-shape fix,
  nested-record implicit `static` (JLS §8.10.3).
- **2026-05-06 → 2026-05-07** — Step 4.5a-c of
  `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`: public Java-model
  interface rollback completed (`resolve(...)`, `resolveAnnotation(...)`,
  `resolveEnumClass(...)`, `containingClassIds`, `isResolved` deleted from
  `core/compiler.common.jvm/.../structure/`).
- **2026-05-04 → 2026-05-05** — Merged refactoring plan landed (PSI removal
  × resolver unification, Stages 1-4); `BinaryJavaClassFinder` follow-ups.
- **2026-04-28 → 2026-04-30** — Test framework wiring fix; PSI-removal Phase 1
  (`BinaryJavaClassFinder` behind `kotlin.javaDirect.useBinaryClassFinder`
  flag, default-OFF in production); shared-FIR PSI-path regression gating.

For full root-cause analyses, fixes, and test results, see
`implDocs/archive/ITERATION_RESULTS_2026_05_11.md`.

### Entry Template

```markdown
## [Title] — [Date]

### Overview
[1-2 sentence summary of what was done and why.]

### Changes
[Bullet list of concrete changes: file, what changed, why.]

### Test Results
[Suite name, pass/fail count, any regressions.]

### Files Modified
| File | Change |
|------|--------|
| `file.kt` | description |

### Key Learnings
[Bullet list of non-obvious findings useful for future work.]
```

> **Add new entries below this line.** Most recent first. Separate with `---`.

---

## Lombok-plugin compatibility with `java-direct` — 2026-05-20

### Overview

Lombok K2 plugin tests (`:kotlin-lombok-compiler-plugin:test`,
`FirLightTreeBlackBoxCodegenTestForLombokGenerated`) regressed 13 / 66 after the
Stage-1.5 / 1.6 direct-injection wiring (`ac8736eae6a8`, see
`implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md`) made the CLI test pipeline use
`java-direct` unconditionally. Two pre-existing `java-direct` gaps surfaced under
the Lombok plugin path; both fixed without touching `JvmFrontendPipelinePhase`,
keeping `java-direct` unconditional in CLI.

Pre-Stage-1.5, Lombok tests went through `JvmFrontendPipelinePhase`'s
`projectEnvironment.getFirJavaFacade(...)` which fell back to PSI when no
`JavaClassFinderFactory` extension was registered — Lombok tests never
registered `JavaDirectPluginRegistrar`, so they were on PSI. Post-Stage-1.5,
`JvmFrontendPipelinePhase` unconditionally hands `java-direct` to every
`createSourceSession` / `createLibrarySession` invocation, including those of
the `FirCliJvmFacade` test fixture that Lombok inherits via
`AbstractFirLightTreeBlackBoxCodegenTest` → `AbstractJvmBlackBoxCodegenTestBase`
→ `setupJvmPipelineSteps`.

### Root cause

Two independent bugs, both observable only when `java-direct` loads a Java
source file whose top-level annotations / fields participate in a Lombok plugin
generator (`AccessorGenerator`, `LombokConstructorsGenerator`, `BuilderGenerator`,
…).

1. **Single-segment star-import recovery missed in
   `JavaImportResolver.extractFragmentedImports`.** Lombok test data starts every
   `.java` block with a `// FILE: …` comment placed *above* the import line:

   ```java
   // FILE: ConstructorExample.java

   import lombok.*;

   @NoArgsConstructor public class ConstructorExample { … }
   ```

   The Java light-tree parser fragments this shape into root-level siblings
   instead of populating `IMPORT_LIST`:

   ```
   root children = [IMPORT_LIST (empty),
                    END_OF_LINE_COMMENT,
                    ERROR_ELEMENT(IMPORT_KEYWORD),
                    MODIFIER_LIST,
                    TYPE("lombok."),
                    ERROR_ELEMENT(""),
                    ERROR_ELEMENT("*;"),
                    CLASS]
   ```

   `extractFragmentedImports` recovers the star import via `findTypeNodeAndStar`,
   trims the trailing `.` from `"lombok."` → `"lombok"`, but the final guard
   `if (fqName.contains('.'))` was dropping it because the package name has no
   dot. Result: `star imports = []`, `JavaResolutionContext.resolve("NoArgsConstructor")`
   returns `null`, `JavaAnnotationOverAst.classId` falls back to
   `ClassId.topLevel(FqName("NoArgsConstructor"))` =
   `ClassId(root, NoArgsConstructor)`. Lombok's `getAnnotationByClassId(ClassId(lombok, NoArgsConstructor), …)`
   does not match → generator returns null → no constructor / getter / setter
   generation.

2. **`JavaField.hasInitializer` missing from the model.** Lombok's K2
   constructor-generator parts (`AllArgsConstructorGeneratorPart`,
   `RequiredArgsConstructorGeneratorPart`) need "does this field carry an
   initializer expression (constant or not)?" The previous implementation cast
   `declaration.source?.psi as? PsiField` and called `psiField.hasInitializer()`.
   For `java-direct`-loaded fields, `source.psi` is `null` → cast returns
   `null` → `hasInitializer = false`. Lombok then *includes* fields like
   `private Long zzzz = 23L` (non-constant per JLS 4.12.4 — `Long` is a
   reference type) in the generated constructor, producing
   `ConstructorExample(String foo, boolean otherField, Long zzzz)` instead of
   the expected `ConstructorExample(String foo, boolean otherField)`. The cast
   was itself a PSI leak in the K2 plugin path.

### Fixes

**(a) `JavaImportResolver.extractFragmentedImports`** — relax the FQN guard:
non-star imports still require a dot (`import Foo;` is illegal Java and not
recovered), but **star imports** accept single-segment package names. Code:

```kotlin
if (fqName.isNotEmpty()) {
    if (target.hasStar) {
        // Single-segment star imports (`import lombok.*;`) are valid Java;
        // the dot guard would have wrongly skipped them.
        starImports.add(FqName(fqName))
    } else if (fqName.contains('.')) {
        val simpleName = fqName.substringAfterLast('.')
        simpleImports.putIfAbsent(simpleName, FqName(fqName))
    }
}
```

**(b) `JavaField.hasInitializer: Boolean`** — new public property on the
`core/compiler.common.jvm/.../load/java/structure/JavaField` interface.
**Rule §7 exception** ([`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md)):
adding a member to a Java-model interface. Justification:

- **Net debt reduction**, not addition. The Lombok K2 plugin previously cast
  `source.psi as? PsiField` to reach `hasInitializer()`. That cast is itself a
  PSI leak into K2 plugin code; replacing it with a model-level property
  *removes* PSI from the plugin call path.
- **PSI must implement** `hasInitializer` too, because the K1→K2 migration
  expects Lombok K2 to work over PSI-loaded fields with non-constant
  initializers (e.g. `final String x = computeX();`) — a `java-direct`-private
  subinterface in `compiler/fir/fir-jvm/JavaModelExtensions.kt` would cover
  only the `java-direct` arm and regress the PSI arm. The cleanest shape is
  the same property on both impls, plus binary and javac-wrapper for
  completeness.

Semantics: `true` iff the source/binary declaration carries any initializer
expression, broader than the existing `hasConstantNotNullInitializer` (which
restricts to JLS 4.12.4 compile-time constants).

Implementations:

| Impl | Body | Notes |
|------|------|-------|
| `JavaFieldImpl` (PSI) | `getPsi().hasInitializer()` | The K1 PSI behavior — now also reachable from K2 without the PSI cast leak. |
| `JavaFieldOverAst` (`java-direct`) | `initializerNode != null` | The existing private `initializerNode: JavaLightNode?` is non-null iff `= …` follows the field name in the AST. |
| `BinaryJavaField` (ASM) | `initializerValue != null` | Class files only encode `ConstantValue` attribute — equivalent to `hasConstantNotNullInitializer`. |
| `TreeBasedField` (javac wrapper, JCTree) | `tree.init != null` | Direct javac access. |
| `SymbolBasedField` (javac wrapper, javax.lang.model) | `initializerValue != null` | Symbol view only sees constant `VariableElement.getConstantValue()`. |
| `MockKotlinField` (javac wrapper, K1 stub) | `shouldNotBeCalled()` | Same shape as other unsupported members on this mock. |
| `ReflectJavaField` (java.lang.reflect) | `false` | Reflection cannot observe initializer expressions. |

`FirJavaField` exposes the property through the existing lazy-property pattern:

- New constructor parameter `lazyHasInitializer: Lazy<Boolean>`, stored as
  `var lazyHasInitializer` (matching `lazyInitializer` / `lazyHasConstantInitializer`).
- New public `val hasInitializer: Boolean get() = lazyHasInitializer.value`.
- `FirJavaFieldBuilder` adds a `lateinit var lazyHasInitializer` and passes it
  to the constructor.
- `FirJavaFacade.createFirJavaField` populates `lazyHasInitializer = lazy { javaField.hasInitializer }`.
- `SignatureEnhancement` (which copies a `FirJavaField` into an enhanced
  variant) propagates `firElement.lazyHasInitializer` on the
  `FirJavaField`-typed branch and synthesises `lazy { firElement.initializer != null }`
  on the generic `FirField` branch.

Lombok K2 generators drop the `PsiField` cast and read `declaration.hasInitializer`
directly:

```kotlin
// AllArgsConstructorGeneratorPart.getFieldsForParameters
if (declaration.hasInitializer && (!isAllArgsConstructor || !declaration.isVar)) continue
```

```kotlin
// RequiredArgsConstructorGeneratorPart.isFieldRequired
if (isStatic) return false
if (hasInitializer) return false
if (isVal) return true
return annotations.any { it.unexpandedClassId?.asSingleFqName() in LombokNames.NON_NULL_ANNOTATIONS }
```

### Test Results

After both fixes:

| Suite | Tests | Failures | Errors |
|-------|------:|---------:|-------:|
| `:kotlin-lombok-compiler-plugin:test` → `FirLightTreeBlackBoxCodegenTestForLombokGenerated` | 66 | 0 | 0 |
| `:compiler:java-direct:test` → `JavaUsingAstBoxTestGenerated`    | 1181 | 0 | 0 |
| `:compiler:java-direct:test` → `JavaUsingAstPhasedTestGenerated` | 1519 | 0 | 0 |

No regressions on `JavaUsingAst*` (which already exercised `java-direct` over
typical annotation patterns). Lombok back to 100% green.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaImportResolver.kt` | `extractFragmentedImports`: relax FQN guard — star imports accept single-segment package names; non-star still require a dot. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt` | `JavaField`: add `val hasInitializer: Boolean`. |
| `compiler/frontend.common.jvm/src/.../load/java/structure/impl/JavaFieldImpl.java` | PSI impl: `getPsi().hasInitializer()`. |
| `compiler/frontend.common.jvm/src/.../load/java/structure/impl/classFiles/Other.kt` | `BinaryJavaField`: `initializerValue != null`. |
| `compiler/java-direct/src/.../model/JavaMemberOverAst.kt` | `JavaFieldOverAst`: `initializerNode != null`. |
| `compiler/javac-wrapper/src/.../wrappers/trees/TreeBasedField.kt` | `tree.init != null`. |
| `compiler/javac-wrapper/src/.../wrappers/symbols/SymbolBasedField.kt` | `initializerValue != null`. |
| `compiler/javac-wrapper/src/.../resolve/KotlinClassifiersCache.kt` | `MockKotlinField`: `shouldNotBeCalled()`. |
| `core/descriptors.runtime/src/.../runtime/structure/ReflectJavaField.kt` | `false`. |
| `compiler/fir/fir-jvm/src/.../declarations/FirJavaField.kt` | New `lazyHasInitializer` constructor param + `hasInitializer` property; builder field. |
| `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` | Populate `lazyHasInitializer = lazy { javaField.hasInitializer }`. |
| `compiler/fir/fir-jvm/src/.../enhancement/SignatureEnhancement.kt` | Propagate `lazyHasInitializer` when copying `FirJavaField`; synthesise on the generic `FirField` branch. |
| `plugins/lombok/lombok.k2/src/.../generators/AllArgsConstructorGeneratorPart.kt` | Drop `PsiField` cast; read `declaration.hasInitializer`. |
| `plugins/lombok/lombok.k2/src/.../generators/RequiredArgsConstructorGeneratorPart.kt` | Drop `PsiField` cast; read `hasInitializer` on the receiver. |

### Key Learnings

- **`// FILE: …` testdata comments break Java parser shapes.** The
  Kotlin-testdata convention of placing `// FILE:` separator comments at the
  top of each `.java` block defeats the parser's `IMPORT_LIST` recognition and
  scatters imports across `ERROR_ELEMENT` siblings of the file root. Any
  recovery path in `JavaImportResolver` must accept single-segment FQN
  recovery for star imports — `import foo.*;` is valid even when `foo` has no
  dots.

- **The Lombok K2 plugin had a hidden PSI dependency.** Before this work,
  `(declaration.source?.psi as? PsiField)?.hasInitializer()` silently returned
  `null` (cast fails) for any non-PSI Java-model impl. K1's
  `RequiredArgsConstructorProcessor` / `AllArgsConstructorProcessor` had the
  same shape. Replacing the cast with a model-level `hasInitializer` property
  *removes* PSI from the K2 plugin call path — a net debt reduction even
  though it required a new member on a `JavaField` interface (rule §7
  exception).

- **`hasConstantNotNullInitializer` is not "has any initializer".** The
  former is restricted to JLS 4.12.4 constant variables (primitive or
  `String`, `final`, initialized to a constant expression). `Long zzzz = 23L`
  *has* an initializer but is *not* a constant variable. Lombok semantics ask
  the broader question, so a separate property is the right model-level fix —
  conflating the two would break enhancement-time / annotation-evaluation
  code that genuinely relies on the JLS-strict definition.

- **`lateinit` fields on `FirJavaField` builders silently regress.** Adding a
  `lateinit var lazyHasInitializer` to the builder is invisible until a
  caller forgets to set it; the resulting
  `UninitializedPropertyAccessException` surfaces hundreds of test methods
  later. The single missed call-site in this iteration was
  `SignatureEnhancement.kt:174` (the `FirField → FirJavaField` copy path).
  Future builder additions should grep all `buildJavaField { … }` blocks
  before committing.

- **CLI test fixtures inherit the CLI pipeline.** `FirCliJvmFacade` runs
  `JvmFrontendPipelinePhase` directly. Any wiring change in
  `JvmFrontendPipelinePhase` immediately affects every black-box codegen /
  phased-diagnostic test that uses `setupJvmPipelineSteps`, including
  unrelated plugins like Lombok. `JvmConfigurationPipelinePhase` (the
  *configuration* phase that used to register `JavaDirectPluginRegistrar`) is
  **not** run by `FirCliJvmFacade`, so configuration-time gates are invisible
  to tests — gates have to be in the frontend phase or earlier.

---

## Archived Iteration History

Earlier entries have been moved to dated archives under `implDocs/archive/`:

- `implDocs/archive/ITERATION_RESULTS_2026_05_11.md` — entries 2026-04-22 →
  2026-05-11 (this archive). Covers post-refactoring cleanup, PSI removal
  (Phase 1-2), merged refactoring plan (Stages 1-4 + 4.5a-c public-interface
  rollback), the IJ-FP regression delta (Cat A-E), and the
  `JavaUsingAst*` test framework wiring fix.
- `implDocs/archive/ITERATION_RESULTS_2026_04_22.md` — full log of Phases A-E
  of `REFACTORING_PLAN_2026_04_21.md`: Phase B regression investigation,
  Phase C measurements, Phase D implementation, Phase E cleanup.
- `implDocs/archive/REFACTORING_PLAN_2026_04_21.md` — the 5-phase plan (A-E).
- `implDocs/archive/MEASUREMENTS_2026_04_22.md` — Phase C measurement data
  (8 hypotheses, 3 corpora, corrected classloader-isolation methodology).
- `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` — earlier
  refactoring steps 1.3-3.6.
- `implDocs/archive/LAZY_PACKAGE_INDEXING_PLAN_2026_04_21.md` — lazy
  per-package indexing design (implemented).
- `implDocs/archive/ITERATIONS_52_71_DETAILS.md` — iterations 52-71
  (2026-03-23 → 2026-04-16): wrong-arity type arguments, transitive
  inherited inner class resolution, performance round (61-65), cross-package
  inherited inner classes, multi-field declarations, and the original
  `JavaResolutionContext` split into collaborators.
- `implDocs/archive/ITERATIONS_37_51_DETAILS.md`,
  `implDocs/archive/ITERATIONS_27_36_DETAILS.md`,
  `implDocs/archive/ITERATIONS_24_26_DETAILS.md`,
  `implDocs/archive/ITERATIONS_17_23_DETAILS.md`,
  `implDocs/archive/ITERATIONS_7_16_DETAILS.md`,
  `implDocs/archive/ITERATIONS_1_6_DETAILS.md` — earlier numbered iterations.

### Open items carried forward

- **Context-level `tryResolve` cache** (`PERFORMANCE_REVIEW_2026-04-20.md` §2 #6)
  — deferred with a recorded correctness argument. Only revisit if profiling
  shows `resolve()` as a measurable bottleneck.
- **Variant D of `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §12 Q1** — the
  `FirJavaClass.javaClass` visibility flip — preserved as a fallback in the
  proposal but not taken; `directSupertypeClassIds()` (Variant C) is shipped.
- **Build-time enforcement that `LazySessionAccess` is the only `ThreadLocal`
  / re-entrance choke-point in resolution code** — a grep gate or detekt rule
  could forbid `ThreadLocal` in `compiler/java-direct/.../resolution/` to avoid
  reintroducing the old per-thread re-entrance pattern.
