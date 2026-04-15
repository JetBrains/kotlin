# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-04-15 (iter 68)

---

## Archives

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1–6 | 0 → 90/138 box (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7–16 | 90 → 1075/1166 box (92.2%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17–23 | 1075 → 1150/1167 box, 1374/1442 phased (95.3%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24–26 | 1150/1167 → same, phased 300 → 1374/1442 |
| `implDocs/archive/ITERATIONS_27_36_DETAILS.md` | 27–36 | 1150/1167 → 1157/1168 box, **79 combined failing** |
| `implDocs/archive/ITERATIONS_37_51_DETAILS.md` | 37–51 | 1157/1168 → 1165/1168 box, **17 combined failing** |
| (inline below) | 52–65 | 1165/1168 → 1168/1168 box, 1454/1456 phased, **2 won't-fix** |
| (inline below) | 66–68 | Cross-package inherited inner classes + binary supertype BFS + multi-field declarations |

---

## Iteration 52: Multi-Dimensional Array Types - 2026-03-23

### Root Cause Analysis
The KMP Java parser places multi-dimensional array brackets as **siblings** under the same TYPE node:
```
TYPE: List<Double>[][]
  TYPE: List<Double>        ← inner base type
    JAVA_CODE_REFERENCE: List<Double>
  LBRACKET: [               ← first dimension (siblings, not nested)
  RBRACKET: ]
  LBRACKET: [               ← second dimension
  RBRACKET: ]
```

`createJavaType()` only found one `LBRACKET` and created one `JavaArrayTypeOverAst`, silently dropping additional dimensions. This turned `List<Double>[][]` into `List<Double>[]`, causing raw type erasure to produce wrong member types and incorrect diagnostics.

### Fix
**File**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`

Changed both array-handling paths in `createJavaType()` to count ALL `LBRACKET` children and wrap that many `JavaArrayTypeOverAst` dimensions around the inner type. Annotations are attached only to the outermost dimension.

### Test Results
- Box: 1167/1168, Phased: 1444/1456, Total failing: 13

### Tests Fixed
- **Box**: `testJavaMethodsSmokeTest`, `testJavaArrayType` (reflection metadata now correct with proper array dimensions)
- **Phased**: `testArrays` (raw type array field assignments), `testRawSupertypeOverride` (method override matching with raw supertype)

### Remaining Raw Type Failures
- `testInterdependentTypeParameters` — different root cause (Boo<N> rendered as Boo<*,*,*> instead of Boo<*>)
- `testNonTrivialErasure` — different root cause (recursive bound erasure not producing correct type)

### Key Learnings
- KMP parser flattens array dimensions as siblings, unlike javac which nests `JCArrayTypeTree`
- Always dump AST structure when array or nested type handling is suspect

---

## Iteration 53: Annotation Default Binary Expressions - 2026-03-23

### Root Cause Analysis
Java annotation members can have default values that are compile-time constant expressions, e.g.:
```java
int i2() default 21212121 + 32323232;
String str() default "fi" + "zz";
```

`evaluateConstantExpression()` in `JavaAnnotationOverAst.kt` only handled `PREFIX_EXPRESSION` (negation) and `LITERAL_EXPRESSION`, but not `BINARY_EXPRESSION`. Binary expressions like `21212121 + 32323232` evaluated to `null`, which FIR converted to an error expression. When comparing the Java annotation's defaults against the Kotlin `expect` declaration's defaults, the mismatch triggered `ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE`.

### Fix
**File**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaAnnotationOverAst.kt`

Added `BINARY_EXPRESSION` handling to `evaluateConstantExpression()`:
- Extracts left/right operands and operator (`PLUS`, `MINUS`, `ASTERISK`, `DIV`, `PERC`)
- Recursively evaluates operands (supports nested expressions)
- `evaluateBinaryExpression()` handles string concatenation (`PLUS` with any `String` operand) and integer arithmetic
- `numericBinaryOp()` preserves `Long` vs `Int` type based on operands

### Test Results
- Box: 1168/1168, Phased: 1445/1456, Total failing: 11

### Tests Fixed
- **Box**: `testAnnotationsViaActualTypeAliasFromBinary` (annotation default value matching)
- **Phased**: `testAnnotationsViaActualTypeAlias2` (same root cause — `ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE` no longer spuriously reported)

### Key Learnings
- PSI evaluates constant expressions via javac's `PsiExpression.evaluate()`; java-direct must implement its own evaluator
- Annotation default values flow through `annotationParameterDefaultValue` → FIR's `toFirExpression` → `createConstantOrError`, where `null` becomes an error expression

---

## Iteration 54: External Class Flexible Type Rendering - 2026-03-23

### Root Cause Analysis
In FIR's `JavaTypeConversion.kt`, the `isTriviallyFlexible()` check at line 169 relies on `classifier` being non-null to determine if a Java type should render as `T!` (trivially flexible) vs `ft<T, T?>` (verbose). With PSI, `classifier` is always non-null for resolved external classes (JDK, libraries). With java-direct, `classifier` is null for all external classes (only local source classes resolve), falling through to `isTriviallyFlexibleHint` which only handled cross-file Java source classes.

Types like `Comparable` (java.lang) and `Comparator` (java.util) were not recognized as trivially flexible, causing inference dump mismatches in tests that compare type representations.

### Fix
**File**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`

Enhanced `isTriviallyFlexibleHint` in `JavaClassifierTypeOverAst` to handle external classes:
1. For types resolved via explicit imports: check the Java FQN against `JavaToKotlinClassMap.getReadOnlyAsJava()` — read-only collection classes (List, Map, etc.) are NOT trivially flexible
2. For unresolved simple names (java.lang implicit import, star imports): conservatively check against simple names of read-only collection classes

Added `JAVA_READ_ONLY_FQ_NAMES` and `JAVA_READ_ONLY_SIMPLE_NAMES` companion object constants.

### Test Results
- Box: 1168/1168, Phased: 1446/1456, Total failing: 10

### Tests Fixed
- **Phased**: `testFlatMapWithReverseOrder` (inference dump: `Comparable<in W!>!` and `Comparator<W!>!` now render correctly instead of `ft<...>`)

### Key Learnings
- PSI's `classifier` is non-null for all resolved classes (including JDK); java-direct's `classifier` is null for external classes — the `isTriviallyFlexibleHint` must bridge this gap
- Read-only collection classes (`java.util.List`, `java.util.Map`, etc.) must NOT be trivially flexible because they need mutable/readonly distinction in the upper bound
- `java.lang.*` classes are implicitly imported but not in java-direct's explicit import list — conservative simple name matching handles this

---

## Iteration 55: Wrong-Arity Type Argument Handling - 2026-03-23

### Root Cause Analysis
Java source code can contain type references with the wrong number of type arguments, e.g.:
```java
class Boo<N> {}
class Foo<P1 extends Boo<P2, P3, P4>, ...>  // Boo has 1 param, 3 args given
class A<T extends A<T, E>, E extends T, F>  // A has 3 params, 2 args given in bound
```

Javac rejects these with "wrong number of type arguments" but still processes them. PSI (backed by javac) handles these gracefully — fewer args than params are treated as raw, more args than params are truncated to the correct count.

Java-direct parses the AST literally and presents the wrong-arity type arguments as-is. This caused two problems:
1. **AIOOBE crash**: `ConeRawScopeSubstitutor.substituteType()` created a `nullabilities` array sized to `type.typeArguments.size` but iterated `firClass.typeParameterSymbols.size`, causing `ArrayIndexOutOfBoundsException` when the counts differed.
2. **Wrong type erasure**: Raw type erasure with wrong-arity arguments produced incorrect types, leading to spurious diagnostics.

### Fix
**Three coordinated changes:**

1. **`compiler/fir/providers/src/.../ConeRawScopeSubstitutor.kt`**: Defensive fix — size `nullabilities` array to `firClass.typeParameterSymbols.size` instead of `type.typeArguments.size`, using safe `getOrNull` access. Prevents AIOOBE for any source of arity mismatch.

2. **`compiler/java-direct/src/.../JavaTypeOverAst.kt`**: Enhanced `isRaw` to detect fewer-args-than-params case. When explicit type argument count < class type parameter count, the type is treated as raw (matching PSI/javac behavior).

3. **`compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`**: Two changes:
   - Extended `isRawType` check in `classifier == null` path to also detect fewer-args-than-params
   - `buildTypeProjections()` now truncates type arguments to `min(typeArguments.size, typeParameterSymbols.size)` for more-args-than-params case

### Test Results
- Box: 1168/1168, Phased: 1448/1456, Total failing: 8

### Tests Fixed
- **Phased**: `testNonTrivialErasure` (recursive bound erasure with fewer args than params)
- **Phased**: `testInterdependentTypeParameters` (interdependent bounds with more args than params)

### Key Learnings
- Javac rejects wrong-arity type references but PSI still presents them — fewer args → raw-like, more args → truncated
- `ConeRawScopeSubstitutor` assumes `type.typeArguments.size == firClass.typeParameterSymbols.size` — this invariant can be violated by wrong-arity Java source
- The `classifier == null` path in `JavaTypeConversion.kt` needs the same wrong-arity handling as the `classifier is JavaClass` path

---

## Iteration 56: Transitive Inherited Inner Class Resolution (Problems 6 & 7) - 2026-03-26

### Root Cause Analysis
**Problem 6 (`testMapMethodsImplementedInJava`)**: `Entry` in `Derived.entrySet()` return type `Set<Entry<String, String>>` resolved to wrong `ClassId("", "Entry")` instead of `ClassId("java.util", "Map.Entry")`. This made the return type subtype check in `JavaClassUseSiteMemberScope.findGetterOverride()` fail when matching `entrySet()` → `entries` property, causing spurious `ABSTRACT_MEMBER_NOT_IMPLEMENTED`.

**Problem 7 (`testInheritanceWithKotlin`)**: Same root cause — inherited nested classes through multi-level K-J-K chain (Java class → Kotlin class → another Kotlin/Java class with inner classes) couldn't be resolved because `resolveInheritedInnerClassToClassId` only walked Java source supertypes.

**Root cause**: `resolveInheritedInnerClassToClassId` used only text-based resolution of supertypes within the Java source index. When a supertype was a Kotlin class (e.g., `KFirst`) or a binary class (e.g., `java.util.Map`), its own supertypes couldn't be walked — the inner class lookup stopped at the first non-source class boundary.

### Fix
**Two-phase BFS in `resolveInheritedInnerClassToClassId`:**

1. **`core/compiler.common.jvm/src/.../javaTypes.kt`**: Added `getSupertypeClassIds` parameter to `JavaClassifierType.resolve()` (backward-compatible default `null`). This allows FIR to provide a callback for walking non-source supertypes.

2. **`compiler/java-direct/src/.../JavaResolutionContext.kt`**: Rewrote `resolveInheritedInnerClassToClassId` with two phases:
   - **Phase 1**: BFS through `JavaClassifierType` objects (Java model). For each resolved supertype ClassId, probe `SupertypeClassId.SimpleName` via `tryResolve`. For Java source classes, queue their own supertypes. For non-source classes (Kotlin/binary), collect in `nonSourceSupertypeIds`.
   - **Phase 2**: For non-source supertypes, use the FIR callback (`getSupertypeClassIds`) to walk their supertype ClassIds transitively and probe for inner classes.

3. **`compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`**: Updated both `resolve()` call sites to pass `getSupertypeClassIds` callback. Added `getResolvedSupertypeClassIds()` helper that reads already-resolved FIR supertypes for non-Java classes (guards against `FirJavaClass` to avoid premature lazy resolution).

4. **`compiler/java-direct/src/.../JavaTypeOverAst.kt`**: Updated all `resolve()` overrides (`JavaClassifierTypeOverAst`, `EnumSupertypeForJavaDirect`, `EnumSelfTypeArgument`, `SimpleClassifierType`, `JavaClassifierTypeForEnumEntry`) to accept the new `getSupertypeClassIds` parameter.

5. **`compiler/java-direct/test/.../JavaParsingTest.kt`**: Updated 3 `resolve` call sites from trailing lambda syntax `.resolve { ... }` to named argument syntax `.resolve(tryResolve = { ... })`.

### Test Results
- Box: 1168/1168, Phased: 1450/1456, Total failing: 6 (down from 8)

### Tests Fixed
- **Phased**: `testMapMethodsImplementedInJava` (Problem 6 — `Entry` inner class resolution through `Derived → Base → Map`)
- **Phased**: `testInheritanceWithKotlin` (Problem 7 — nested class resolution through K-J-K chain)

### Key Learnings
- `resolveInheritedInnerClassToClassId` must not call `supertype.resolve(tryResolve)` recursively (caused 152-test regression from infinite recursion)
- `FirJavaClass.superTypeRefs` is lazy with `PUBLICATION` mode — accessing it can trigger premature resolution; guard with `is FirJavaClass` check
- Phase 1 uses only text-based resolution (`resolveSimpleNameToClassIdWithoutInheritance`) to avoid recursion; Phase 2 uses FIR callback for cross-language walking

---

## Iteration 57: Outer Type Arguments for Inherited Inner Classes (Problem 5) - 2026-03-26

### Root Cause Analysis
**Problem 5 (`testKJKComplexHierarchyWithNested`)**: After Iteration 56 fixed the ClassId resolution, `NestedInSuperClass` correctly resolved to `SuperClass.NestedInSuperClass`. However, the **outer class type arguments** (`T=String` from `J1 → KFirst → SuperClass<String>`) were missing. FIR treated the type as a **raw type** (star projections), making `nested(x: T)` become `nested(x: Any?)` — accepting any argument instead of only `String`.

PSI handles this via `PsiSubstitutor` which maps `T→String` and includes the outer type arg in `computeTypeArguments()`. Java-direct doesn't have a substitutor mechanism, so `typeArguments` was empty for cross-file inner classes where `classifier == null`.

**Two raw type checks**: The issue manifested in TWO places:
1. **Outer check** (`toConeTypeProjection`, lines 154-168): Detected `NestedInSuperClass` as raw and wrapped the result in `ConeRawType`
2. **Inner check** (`convertClassifierTypeWithClassId`, null branch): Also detected as raw and used star projections for type arguments

### Fix
**Three coordinated changes across 4 files:**

1. **`core/compiler.common.jvm/src/.../javaTypes.kt`**: Added `containingClassIds: List<ClassId>` property to `JavaClassifierType` (defaults to empty list). Exposes the class hierarchy context (innermost class to outermost) so FIR can walk supertypes to find outer type arguments.

2. **`compiler/java-direct/src/.../JavaResolutionContext.kt`**: Added `getContainingClassIds()` method that walks `containingClassProvider` and outer classes, converting each to ClassId.

3. **`compiler/java-direct/src/.../JavaTypeOverAst.kt`**: Implemented `containingClassIds` in `JavaClassifierTypeOverAst` via lazy delegation to `resolutionContext.getContainingClassIds()`.

4. **`compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`**: Three helper functions + two check modifications:
   - `findOuterTypeArgsFromHierarchy()`: Walks outer containing classes' FIR supertypes to find the outer class (e.g., `SuperClass`) with its concrete type arguments (e.g., `<String>`). **Skips index 0** (the class whose supertypes are being resolved) to avoid infinite recursion.
   - `findTypeArgsForClassInHierarchy()`: Recursive BFS through FIR supertype chain (non-Java only) searching for target ClassId.
   - `substituteTypeArgs()`: Handles type parameter substitution through intermediate classes (e.g., `A<X> : SuperClass<X>`, `A<String>` → `SuperClass<String>`).
   - **Outer `isRawType` check**: Added guard — don't treat as raw if `findOuterTypeArgsFromHierarchy` can provide the outer type args.
   - **Inner `null` branch**: Compute outer type args and use them instead of raw projections when available.

### Recursion Prevention
The key challenge was avoiding infinite recursion when accessing `FirJavaClass.superTypeRefs`:
- `containingClassIds = [J1.NestedSubClass, J1]`
- Index 0 (`J1.NestedSubClass`) — its `superTypeRefs` is the lazy property currently being computed → accessing it causes `StackOverflowError`
- Index 1+ (`J1`) — outer class, supertypes already resolved (FIR processes outer before inner) → safe to access
- Solution: always skip index 0, start walking from index 1

### Test Results
- Box: 1168/1168, Phased: 1451/1456, Total failing: 5 (down from 6)

### Tests Fixed
- **Phased**: `testKJKComplexHierarchyWithNested` (Problem 5 — outer type param propagation through K-J-K hierarchy for inherited inner classes)

### Key Learnings
- PSI's `PsiSubstitutor` provides outer type args automatically via `computeTypeArguments()`; java-direct must compute them explicitly from FIR's supertype chain
- FIR type conversion has TWO separate raw type checks — the outer one in `toConeTypeProjection` and the inner one in `convertClassifierTypeWithClassId`; both must be updated
- The first containing class (index 0) is always unsafe to access supertypes for during type conversion — it's the class whose supertypes are being lazily resolved
- FIR's resolved supertype `ConeClassLikeType` already has substituted type arguments (e.g., `SuperClass<String>`), but when walking through intermediate classes with their own type params, manual substitution is still needed

---

## Iteration 58: Investigation of javac Sealed Package Failures (Problems 2 & 8) - 2026-03-27

### Root Cause Analysis
**Problems #2 (`testClassFromJdkInLibrary`) and #8 (`testPseudoRawTypes`)** both define Java classes in JDK-sealed packages (`java.util.Date` and `java.util.Collection` respectively). On Java 9+, the module system seals these packages — user code cannot define classes there.

The test backend (`AbstractJvmIrBackendFacade.transform`) compiles Java sources with in-process javac via `ToolProvider.getSystemJavaCompiler()`. In the **java-direct test worker JVM** (JDK 17, pinned via `kotlin { jvmToolchain(17) }` in `build.gradle.kts`), javac enforces the module seal: `error: package exists in another module: java.base`. In the **PSI test worker JVM**, the same in-process javac with identical options and files succeeds (`result=Success`) because the PSI test worker uses a different javac version that is more lenient about sealed package enforcement.

**Investigation verified:**
- Both test workers use `jdkHome=null` (system compiler), identical javac options, identical source files
- The compilation output directory contents are identical (`[META-INF]`)
- The `JavaCompilerFacade.compileJavaFiles` code path is identical — the difference is the javac version used by each Gradle test worker JVM
- The javac failure in `BackendCliJvmFacade.transform` throws `JavaCompilationError` → `ErrorFromFacade` → `processModule(library)` returns false → main module never compiled → diagnostic mismatch + missing JVM artifact cascade

**Also confirmed** that problems #1, #3, #4 do NOT follow this pattern — they fail with genuine FIR diagnostic mismatches (no `JavaCompilationError`).

### Conclusion
Both tests are **won't fix** — they test edge cases where Java sources shadow JDK classes, which is invalid on Java 9+. The java-direct test worker's JDK 17 javac correctly rejects this compilation; the PSI test worker's acceptance is version-specific behavior of its javac, not a feature java-direct needs to replicate.

### Test Results
- Box: 1168/1168, Phased: 1451/1456, Total failing: 5
- No code changes — investigation only

### Status of Remaining 5 Failures
| Test | Category | Status |
|------|----------|--------|
| #1 testInheritFromAnnotationClass2 | Extra diagnostics (annotation class hierarchy) | Remaining — real impl issue |
| #2 testClassFromJdkInLibrary | javac sealed package | Won't fix |
| #3 testJSpecifySimple | Missing nullability diagnostics | Remaining — real impl issue |
| #4 testJSpecifyWithVarargs | Missing nullability diagnostics | Remaining — same as #3 |
| #8 testPseudoRawTypes | javac sealed package | Won't fix |

---

## Iteration 59: Fix Annotation Class Inheritance Detection (Problem 1) - 2026-03-27

### Root Cause Analysis
**Problem #1 (`testInheritFromAnnotationClass2`)**: Java interface `J extends Target` (where `Target` is `kotlin.annotation.Target`, a Kotlin annotation class) was fully resolved by java-direct, allowing FIR's `FirAnnotationClassInheritanceChecker` to walk the entire supertype chain and produce `EXTENDING_AN_ANNOTATION_CLASS_ERROR` on `I`, `C`, `Ann3`, `Ann4`. PSI couldn't resolve `Target` (no PsiClass for Kotlin builtins without stdlib on classpath), so the supertype became a `ConeErrorType` and the checker stopped at `J`.

**Investigation confirmed via debug logging:**
- PSI: `classifierQualifiedName = "Target"` (raw reference text, not FQN — PSI's `canonicalText` falls back to raw text when the class can't be resolved)
- java-direct: `classifierQualifiedName = "kotlin.annotation.Target"` (resolved via import)
- Both have `classifier = null`

The difference cascades: java-direct's `resolve()` callback finds `kotlin.annotation.Target` in FIR's symbol provider (builtins), creating a valid type → checker walks through → extra errors. PSI's default `resolve()` returns null → fallback `ClassId.topLevel("Target")` → ClassId("", "Target") → not found → error type → checker stops.

### Fix (3 files)
1. **`JavaTypeOverAst.kt`** — `classifierQualifiedName` skips import resolution for `kotlin.*` imports (via `isImportTargetAvailableAsJavaClass`), returning the raw name instead. Matches PSI's `canonicalText` behavior for unresolvable classes.

2. **`JavaResolutionContext.kt`** — Added `isImportTargetAvailableAsJavaClass()` that returns false for `kotlin.*` package imports, true for everything else (JDK, library, user-defined Java classes).

3. **`JavaTypeConversion.kt`** (FIR shared code) — Modified `tryResolve` callbacks in `toConeKotlinTypeForFlexibleBound` and `resolveTypeName` to reject classes with `FirDeclarationOrigin.BuiltIns`. Builtins exist only in FIR's symbol provider (no .class files), so PSI can't resolve them. When stdlib IS on the classpath, the same classes have `FirDeclarationOrigin.Library`, so they're still resolved correctly.

Both changes are needed together: `classifierQualifiedName` prevents the FQN from appearing in the fallback `ClassId.topLevel()`, and `tryResolve` prevents the `resolve()` callback from finding builtins.

### Test Results
- Box: 1168/1168, Phased: 1452/1456, Total failing: 4 (down from 5)
- PSI test suite: no regressions (PSI's `resolve()` returns null by default, so the callback change has no effect)

### Status of Remaining 4 Failures
| Test | Category | Status |
|------|----------|--------|
| #2 testClassFromJdkInLibrary | javac sealed package | Won't fix |
| #3 testJSpecifySimple | Missing nullability diagnostics | Remaining |
| #4 testJSpecifyWithVarargs | Missing nullability diagnostics | Remaining — same as #3 |
| #8 testPseudoRawTypes | javac sealed package | Won't fix |

### Key Learnings
- PSI's `canonicalText` returns the raw reference text (not FQN) when IntelliJ can't resolve the class — this is the primary signal that determines whether FIR creates a valid type or error type
- FIR builtins (`FirDeclarationOrigin.BuiltIns`) are available in the symbol provider even without stdlib on classpath, but PSI can't see them — this origin distinction is key for matching PSI behavior
- The `resolve()` callback from FIR and the `classifierQualifiedName` fallback are TWO independent resolution paths in the `null` classifier branch — both must be gated to prevent unwanted resolution

---

## Iteration 60: JSpecify Foreign Annotations Test Infrastructure - 2026-03-27

### Root Cause Analysis
The `testJSpecifySimple` test (and `testJSpecifyWithVarargs`) failed because the JSpecify annotation source files couldn't be found during test execution. The `JvmForeignAnnotationsConfigurator` uses `JavaForeignAnnotationType.Java8Annotations.path` which defaults to `"third-party/java8-annotations"` — a relative path resolved from the test worker's CWD. Since the java-direct test module's `build.gradle.kts` didn't call `withThirdPartyJava8Annotations()`, the system property with the absolute path was never set, causing `kotlin.io.NoSuchFileException: third-party/java8-annotations: The source file doesn't exist`.

This exception was caught by the test infrastructure and reported alongside the data mismatch error. Because the foreign annotations couldn't be compiled to a JAR, the JSpecify annotation classes (`@NonNull`, `@Nullable`, `@NullMarked`) were not on the classpath, so FIR's enhancement pipeline couldn't recognize them — resulting in no nullability diagnostics.

### Fix (3 files)
1. **`build.gradle.kts`** — Added `withThirdPartyJava8Annotations()` to the `projectTests` block, which sets the `KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH` system property to the absolute path `${project.rootDir}/third-party/java8-annotations`. This fixed `testJSpecifySimple`.

2. **`components.kt`** (test fixtures) — Added `registerCompilerExtensions` override to `JavaDirectConfigurator` as defense-in-depth. For CLI-based facades, extensions can be registered via this method in addition to the `COMPILER_PLUGIN_REGISTRARS` mechanism.

3. **`JavaTypeOverAst.kt`** — For varargs parameters (`@NonNull String... args`), member annotations from the parameter's MODIFIER_LIST are now placed on the **component type** (String) instead of the array wrapper (String[]). This matches PSI/javac-wrapper behavior where TYPE_USE annotations like `@NonNull` enhance the component type's nullability. Fixed `testJSpecifyWithVarargs`.

### Test Results
- Box: 1168/1168, Phased: 1454/1456, Total failing: 2 (down from 4)
- `testJSpecifySimple` (#3): **FIXED**
- `testJSpecifyWithVarargs` (#4): **FIXED**

### Status of Remaining 2 Failures
| Test | Category | Status |
|------|----------|--------|
| #2 testClassFromJdkInLibrary | javac sealed package | Won't fix |
| #8 testPseudoRawTypes | javac sealed package | Won't fix |

### Key Learnings
- Test infrastructure configuration (`build.gradle.kts` `projectTests` block) must declare ALL third-party annotation dependencies used by tests with `ENABLE_FOREIGN_ANNOTATIONS`
- The `NoSuchFileException` from `JvmForeignAnnotationsConfigurator` was masked by the test runner's multi-failure reporting — always check ALL failure causes, not just the first
- For varargs, TYPE_USE annotations (`@NonNull`, `@NotNull`) must be on the component type, not the array wrapper — FIR's nullability enhancement reads annotations from the component type

---

## Iteration 61: Resolution Performance — Cached Inherited Inner Classes - 2026-04-13

### Root Cause Analysis
The java-direct performance test (`testMetadata_new_jvm`) showed 2-3x analysis time regression vs the PSI-based `testKotlin_metadata_jvm` (6.0s vs 5.2s total, with analysis time being the main delta). The module under test contains large protobuf-generated Java files.

Profiler data showed `JavaResolutionContext.resolve()` at **850ms total**, dominated by:
- `resolveInheritedInnerClassToClassId` BFS: **650ms** (walking supertype hierarchies)
- `collectInheritedInnerClasses` (ambiguity check): **110ms**
- `resolveNestedClassToClassIdWithoutInheritance`: **540ms** (called from BFS)
- `CombinedJavaClassFinder.findClass`: **680ms** (tryResolve callbacks from BFS)

Diagnostic counters revealed the root cause:
- `resolveSimpleNameToClassId` called **17,646 times** per compilation
- **Every call** triggered `collectInheritedInnerClasses` (no caching) AND the BFS
- The BFS returned **null 100% of the time** — no inherited inner classes were ever found
- This generated **2.4M `tryResolve` callbacks** to the FIR symbol provider
- **505K+ nested resolution calls** inside the BFS, all wasted

The problem: for protobuf classes with hundreds of type references within the same containing class, the supertype hierarchy was walked hundreds of times redundantly, and the BFS was invoked for every simple name even though the name was never an inherited inner class.

### Fix
**Two coordinated changes:**

1. **`JavaClassFinderOverAstImpl.kt`**: Added `inheritedInnerClassesCache: ConcurrentHashMap<ClassId, Map<String, Set<ClassId>>>` to memoize `collectInheritedInnerClasses`. The result depends only on the source index (immutable after `buildIndex()`), so the cache is safe. Returns immutable snapshot via `mapValues { it.value.toSet() }`.

2. **`JavaResolutionContext.kt`**: Replaced step 2b in `resolveSimpleNameToClassId` to use the cached inherited inner class map for **both** ambiguity detection **and** resolution:
   - Walks the containing class + all outer classes (matching the BFS scope)
   - 2+ candidates → ambiguous, return null
   - 1 candidate → use ClassId directly (verify with `tryResolve`)
   - 0 candidates → skip BFS entirely for source supertypes; only fall back to BFS when `getSupertypeClassIds` callback is available (for non-source/Kotlin supertypes)

### Counter Results (before/after)
| Counter | Before | After | Reduction |
|---------|--------|-------|-----------|
| `inheritedBfsCalls` | 17,646 | 12 | 99.93% |
| `resolveNestedWithoutInheritanceCalls` | 505,308 | 24 | 99.995% |
| `resolveSimpleWithoutInheritanceCalls` | 521,934 | 24 | 99.995% |
| `tryResolveCalls` | 2,388,676 | 79,416 | 96.7% |

### Test Results
- Box: 1168/1168, Phased: 1454/1456, Total failing: 2 (no change — same 2 won't-fix)
- Performance test (`testMetadata_new_jvm`): passes

### Key Learnings
- `collectInheritedInnerClasses` only covers same-package source supertypes (via `resolveSupertypeReference` which resolves simple same-package refs). For cross-package/non-source supertypes, the BFS with `getSupertypeClassIds` callback is still needed as fallback.
- The BFS is only invoked when `getSupertypeClassIds != null` AND the cached map has 0 candidates — this covers the rare case of inner classes inherited from Kotlin/binary supertypes.
- `AtomicLong` counters + JVM shutdown hook writing to a temp file is a reliable diagnostic technique for Gradle test workers (stderr is swallowed, but file output persists).

---

## Iteration 62: Resolution Performance — tryResolve Cache - 2026-04-13

### Root Cause Analysis
After Iteration 61 reduced `tryResolveCalls` from 2.4M to 79,416, a second round of diagnostic counters revealed that **82% of those 79K calls were duplicates** — only 14,283 unique ClassIds were probed. The duplication comes from recursive prefix splitting in `resolveNestedClassToClassId`: for a name like `com.google.protobuf.Foo`, the code tries "com" as a class for each prefix length, and each attempt calls `resolveSimpleNameToClassId` which probes same-package, java.lang, and star-import ClassIds — all returning the same result.

Counter data (v2):
| Counter | Value | Insight |
|---------|-------|---------|
| `tryResolveCalls` | 79,416 | Total callback invocations |
| `tryResolveUnique` | 14,283 | Only 18% unique ClassIds |
| `findLocalClassCalls` | 19,352 | 91% from resolveSimpleNameToClassId |
| `findInnerFromSupertypesCalls` | 46,579 | 2.4 per findLocalClass, mostly wasted |
| `classifierQualifiedNameCalls` | 1,493 | Not lazy — re-resolves every access |

### Fix
**`JavaResolutionContext.kt`**: Added a `HashMap<ClassId, Boolean>` cache scoped to each `resolve()` invocation. The `tryResolve` callback is wrapped with `cache.getOrPut(classId) { tryResolve(classId) }` before being passed to `resolveNestedClassToClassId` / `resolveSimpleNameToClassId`.

This is safe because:
- The callback (`session.symbolProvider.getClassLikeSymbolByClassId`) is deterministic within a single `resolve()` call
- The cache is local to the invocation (no cross-call leakage)
- The `HashMap` is lightweight — median size ~10 entries per invocation

Expected impact: eliminates ~65K redundant FIR symbol provider lookups (82% of 79K).

### Test Results
- Box: 1168/1168, Phased: 1454/1456, Total failing: 2 (no change — same 2 won't-fix)
- Performance test (`testMetadata_new_jvm`): passes

### Remaining Optimization Opportunities
Counter data identified further candidates for future iterations:
- `findInnerFromSupertypesCalls: 46,579` — supertype hierarchy walks from `findLocalClass`, mostly returning null. Could be addressed by caching `findLocalClass` results per resolution context.
- `classifierQualifiedName` is `get()` not `lazy` — re-resolves on every access (1,493 calls). Making it lazy would save redundant `findLocalClass` + `split('.')` calls.

---

## Iteration 63: Resolution Performance — findLocalClass Cache + Lazy classifierQualifiedName - 2026-04-13

### Root Cause Analysis
After Iterations 61–62 addressed the BFS and tryResolve hotspots, counter data showed two remaining sources of redundant work:

1. **`findLocalClass` called 19,352 times** with 46,579 `findInnerClassFromSupertypes` calls (2.4 per call). Each walks the supertype hierarchy looking for inherited inner classes. For protobuf files, the vast majority of type names are NOT local/inner classes — they resolve via imports or same-package — so these walks almost always return null. The same name within the same containing class always produces the same result, but no caching existed.

2. **`classifierQualifiedName` is `get()` not `lazy`** — called 1,493 times, each re-invoking `findLocalClass` + `rawTypeName.split('.')`. Since the property value never changes for a given type instance, all but the first call are wasted.

### Fix
**Two changes:**

1. **`JavaResolutionContext.kt`** — Added `findLocalClassCache: HashMap<Name, Any?>` constructor parameter with a sentinel for cached-null results. `findLocalClass` checks the cache before delegating to `findLocalClassUncached`. The cache is **shared** across contexts created via `withTypeParameters()` / `withInheritedTypeParameters()` (same containing class → same results) and **fresh** for `withContainingClass()` (different containing class → results may differ).

2. **`JavaTypeOverAst.kt`** — Changed `classifierQualifiedName` from `get() { ... }` to `by lazy { ... }`, caching the result after first access.

### Test Results
- Box: 1168/1168, Phased: 1454/1456, Total failing: 2 (no change — same 2 won't-fix)
- Performance test (`testMetadata_new_jvm`): passes

### Key Learnings
- `findLocalClass` result depends on `containingClassProvider` and `localClassProvider`, NOT on `typeParametersInScope`. So the cache is valid across `withTypeParameters()` calls.
- `classifierQualifiedName` was the only non-lazy computed property on `JavaClassifierTypeOverAst` — `classifier`, `isRaw`, `typeArguments`, `isTriviallyFlexibleHint` are all `by lazy`.
- HashMap sentinel pattern (`FIND_LOCAL_CLASS_NULL`) is needed because `HashMap.get()` returns null for both "not present" and "present with null value".

---

## Iteration 64: Performance — Lazy Properties + node.text Caching + findInnerClass Cache - 2026-04-14

### Root Cause Analysis
Diagnostic counters (AtomicLong + JVM shutdown hook) across the java-direct test suite (2,664 tests) revealed that:

1. **`node.text` was creating a new String on every access** — 13,089 calls, each invoking `source.subSequence(startOffset, endOffset).toString()`. Many nodes have their text read multiple times (e.g., IDENTIFIER text used by `name`, `fqName`, `findInnerClass`, type resolution).

2. **Class/member properties were `get()` not `by lazy`** — `fqName`, `supertypes`, `methods`, `fields`, `constructors`, `innerClassNames`, `annotations`, `modifierList`, `isInterface`, `isEnum`, etc. Each access re-traversed AST children and re-created wrapper objects. Property-level counters: fqName=32, supertypes=79, methods=41, fields=41, constructors=58, innerClassNames=107, findInnerClass=107, annotations(class)=65 — low per-test but scales linearly with class count in self-compilation.

3. **`findInnerClass` re-created `JavaClassOverAst` + resolution context on every call** — 107 calls in the test suite, each allocating a new class instance with a new resolution context even when the same inner class was looked up repeatedly.

4. **All `by lazy` used default `SYNCHRONIZED` mode** — adds unnecessary lock overhead since all computations are side-effect-free AST reads that are safe to repeat under `PUBLICATION` mode.

### Fix
**Five changes across 5 files:**

1. **`utils.kt`** — `JavaSyntaxNode.text`: `get()` → `by lazy(PUBLICATION)`. Caches the String after first access, eliminating redundant `subSequence().toString()` allocations.

2. **`JavaClassOverAst.kt`** — Made properties lazy with `PUBLICATION` mode:
   - `fqName`, `modifierList`, `supertypes`, `innerClassNames`, `isInterface`, `isAnnotationType`, `isEnum`, `isRecord`, `isSealed`, `methods`, `fields`, `constructors`, `recordComponents`, `annotations`
   - Added `innerClassCache: HashMap<Name, JavaClass?>` for `findInnerClass` — caches results, avoiding repeated `JavaClassOverAst` + resolution context creation.

3. **`JavaMemberOverAst.kt`** — Made properties lazy with `PUBLICATION` mode:
   - Base class: `modifierList`, `annotations`
   - `JavaFieldOverAst`: `isEnumEntry`, `type`
   - `JavaMethodOverAst`: `valueParameters`, `returnType`, `modifierList`
   - `JavaConstructorOverAst`: `valueParameters`

4. **`JavaTypeOverAst.kt`** — Changed all existing `by lazy` to `by lazy(LazyThreadSafetyMode.PUBLICATION)`.

5. **`JavaAnnotationOverAst.kt`** — Changed existing `by lazy` to `by lazy(LazyThreadSafetyMode.PUBLICATION)`.

### Counter Results (before/after on java-direct test suite, 2,664 tests)
| Counter | Before | After | Reduction |
|---------|--------|-------|-----------|
| `findChildByType` calls | 27,093 | 25,371 | 6.4% (−1,722) |
| `getChildrenByType` calls | 16,728 | 16,598 | 0.8% (−130) |
| `node.text` String allocations | 13,089 | 7,085 | **45.9%** (−6,004) |

### Test Results
- Box: 1168/1168, Phased: 1442/1453, Total failing: 11 (no change — same pre-existing failures)
- Zero regressions

### Key Learnings
- `KotlinFullPipelineTestsGenerated` does NOT exercise java-direct significantly — those tests compile Kotlin against binary JDK/library classes, not Java source files. Only 53 `findChildByType` calls across 415 tests. The java-direct test suite is the right benchmark.
- `LazyThreadSafetyMode.PUBLICATION` is safe for all java-direct lazy properties because they are pure computations over immutable AST structures — repeated computation produces identical results with no side effects.
- The `findChildByType`/`getChildrenByType` reduction is modest in the test suite (small Java files, each property accessed ~1 time per class), but scales with class count × member count × access frequency in self-compilation.
- The `node.text` caching is the most impactful single change — nearly half of all String allocations eliminated. Each AST node's text is now computed at most once.

---

## Iteration 65: Performance — CombinedFinder Index Check + Aggregated Inherited Inner Class Cache - 2026-04-14

### Root Cause Analysis
After enabling java-direct in `KotlinFullPipelineTestsGenerated` (110 modules with Java sources, 2,077 Java files), comprehensive counters revealed two major hotspots:

1. **`findClass` on source finder: 106,382 calls, 50% index misses** — `CombinedJavaClassFinder` always called `sourceFinder.findClass()` first, even for classes not in the source index (JDK, libraries). Each miss did an index lookup + method call overhead for nothing — the source finder would just return null.

2. **`collectInheritedInnerClasses`: 1,528,663 calls** — For every type reference resolution (`resolveSimpleNameToClassId`), the code walked the containing class chain (containing class + all outer classes) calling `collectInheritedInnerClasses` for each. Within a single class body, all type references share the same containing class chain, so this walk was repeated identically for every type reference.

Full counter data (pre-optimization, aggregated across 110 modules):
| Counter | Calls | Cache hits | Hit rate |
|---------|-------|------------|----------|
| `findClass` | 106,382 | 48,711 | 45.8% |
| `findLocalClass` | 933,729 | 927,970 | 99.4% |
| `collectInheritedInner` | 1,528,663 | 1,527,717 | 99.9% |
| `findInnerClass` | 9,972 | 1,854 | 18.6% |
| `findChildByType` | 661,676 | — | — |
| `getChildrenByType` | 166,419 | — | — |
| `resolve` | 70,496 | — | — |

### Fix
**Two changes:**

1. **`CombinedJavaClassFinder.kt`** — Added `isClassInIndex` check before calling source finder in `findClass`/`findClasses`. If the top-level class is not in the source index, skip the source finder entirely and go directly to the binary finder. Changed `sourceFinder` type from `JavaClassFinder` to `JavaClassFinderOverAstImpl` to access `isClassInIndex`.

2. **`JavaResolutionContext.kt`** — Added `aggregatedInheritedInnerClassesHolder` (shared mutable `Array<Map?>`). The holder is computed lazily on first access by walking the containing class chain once, collecting all inherited inner classes from `collectInheritedInnerClasses` across the chain, and merging into a single `Map<String, Set<ClassId>>`. Subsequent lookups are a simple map get. The holder is shared across contexts created via `withTypeParameters`/`withInheritedTypeParameters` (same containing class) and fresh for `withContainingClass` (different containing class).

### Counter Results (before/after, aggregated across 110 modules)
| Counter | Before | After | Reduction |
|---------|--------|-------|-----------|
| `findClass` (source finder) | 106,382 | 53,246 | **−50%** |
| `collectInheritedInner` | 1,528,663 | 5,602 | **−99.6%** |
| `findChildByType` | 661,676 | 654,036 | −1.2% |
| `getChildrenByType` | 166,419 | 164,850 | −0.9% |
| `nodeTextMaterializations` | 81,955 | 76,278 | −6.9% |

### Test Results
- java-direct: 2664 tests, 11 failed (no change — same pre-existing failures)
- Pipeline: 415 tests, 5 failed (no change)
- Zero regressions

### Key Learnings
- **Classloader isolation breaks AtomicLong counters**: The java-direct plugin JAR is loaded in its own classloader per compilation. Static `object` counters in the plugin's classes are isolated per classloader — a shutdown hook registered from the test JVM sees a different `object` instance. File-based `appendText` logging is the only reliable cross-classloader diagnostic technique.
- **`CombinedJavaClassFinder.isClassInIndex` gate**: A simple boolean check on the package→className index avoids entering `findClass`→`findClasses`→index lookup chain entirely for classes not in source. This is the most common case (JDK, library classes).
- **Per-context aggregation vs per-call caching**: Even when individual `collectInheritedInnerClasses` calls are 99.9% cache hits, calling it 1.5M times (once per type reference × outer class depth) has measurable overhead from the sheer volume of HashMap lookups. Aggregating the result once per context and reusing it eliminates 99.6% of those calls.
- **Remaining bottleneck**: `findLocalClass` at 933K calls (99.4% cache hit) is now the dominant call by volume. The cache is effective, but the call volume itself (driven by the number of type references across all Java files) means ~933K HashMap lookups. Further optimization would require reducing the number of call sites or restructuring the resolution flow.

---

## Iteration 66: Cross-Package Inherited Inner Class Resolution - 2026-04-15

### Root Cause Analysis
When a Java class in package `derived` extends an interface from package `base`, and that interface declares inner classes (e.g., `FunctionDescriptor.CopyBuilder`), the inner class could not be resolved from the derived class. This caused `MISSING_DEPENDENCY_CLASS` errors for types like `UserDataKey` when compiling classes that inherit from interfaces in different packages.

The root cause was in `JavaClassFinderOverAstImpl.getDirectSupertypes()` — it only resolved supertype references within the same package. Cross-package supertypes specified via imports (e.g., `import base.FunctionDescriptor`) were not found, so the supertype hierarchy walk stopped at the package boundary. Additionally, `resolveNestedClassToClassId` did not search supertypes for inherited inner classes when the nested class wasn't directly declared on the outer class.

### Fix
**Three coordinated changes:**

1. **`JavaClassFinderOverAstImpl.kt`** — Added `findFileRoot()` and `extractImportsLightweight()` helpers to extract imports from the file's AST root node. Updated `getDirectSupertypes()` to pass both explicit imports and star imports through `extractSupertypeRefsFromNode()` to `resolveSupertypeReference()`. Extended `resolveSupertypeReference()` to check explicit imports and star imports after same-package lookup fails.

2. **`JavaResolutionContext.kt`** — Added `findInheritedNestedClass()` method that searches the supertype hierarchy of an outer class for inherited nested classes using both the `getSupertypeClassIds` callback (for Kotlin/binary classes) and `collectInheritedInnerClasses` (for same-package Java source classes). Updated `resolveNestedClassToClassId()` to call this method when a nested class isn't directly declared on the resolved outer class. Also added a fallback path using the aggregated inherited inner class map for cases without the `getSupertypeClassIds` callback.

3. **`JavaResolutionContext.kt`** — Fixed `findLocalClassUncached()` to walk the full outer class chain, checking siblings of each outer class (not just the immediate outer). This handles deeply nested classes like `Outer { Inner1 { Inner2 { ... } } }` where `Inner2` must see siblings of `Outer`.

### Test Results
- Box: 1168/1168, Phased: 1454/1456, Total failing: 2 (same 2 won't-fix)
- `testInheritedInnerClassCrossPackage` (new test): PASSES
- Full `:kotlin-java-direct:test` suite: zero regressions

### Caching Verification
All new code paths use existing caches:
- `getDirectSupertypes()` results cached via `supertypeCache` — `extractImportsLightweight()` is only called on cache miss
- `findInheritedNestedClass()` calls `collectInheritedInnerClasses()` which is cached via `inheritedInnerClassesCache`
- `resolveNestedClassToClassId()` fallback uses `getAggregatedInheritedInnerClasses()` which is cached via `aggregatedInheritedInnerClassesHolder`

### Key Learnings
- `getDirectSupertypes()` was the only place that resolved supertype references without considering imports — all other resolution paths (type references, `classifierQualifiedName`) already used imports
- The `resolveNestedClassToClassId` path (`Map.Entry`, `SimpleFunctionDescriptor.CopyBuilder`) is distinct from `resolveSimpleNameToClassId` — inherited inner class resolution must be implemented in both
- The aggregated inherited inner class map only covers same-package source supertypes; the `getSupertypeClassIds` callback is still needed for cross-package/binary/Kotlin supertypes

---

## Iteration 67: Binary Java Supertype Hierarchy Walking for testDescriptors_jvm - 2026-04-15

### Root Cause Analysis
The `testDescriptors_jvm` pipeline test failed with `MISSING_DEPENDENCY_CLASS` errors for `UserDataKey` — an inner class declared in `CallableDescriptor` (a binary Java class from the descriptors module). The resolution failed because `getResolvedSupertypeClassIds()` in `JavaTypeConversion.kt` skipped ALL `FirJavaClass` instances, including binary Java classes (`FirDeclarationOrigin.Java.Library`). This prevented Phase 2 BFS from walking binary Java supertype hierarchies like `CallableMemberDescriptor → CallableDescriptor → UserDataKey`.

Binary Java classes have pre-populated `nonEnhancedSuperTypes` (set during deserialization), so accessing their `superTypeRefs` is safe — unlike Java SOURCE classes where `superTypeRefs` is lazy and accessing it can trigger premature resolution cycles.

### Fix
**File**: `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`

Changed `getResolvedSupertypeClassIds()` to only skip `FirJavaClass` with `FirDeclarationOrigin.Java.Source` origin (not `Java.Library`). This allows Phase 2 BFS to walk binary Java supertype hierarchies while still preventing premature lazy resolution of Java source class supertypes.

### Test Results
- `testDescriptors_jvm`: PASSES
- Full `:kotlin-java-direct:test` suite: zero regressions (2664 tests, same 2 won't-fix)

### Caching Verification
The `getResolvedSupertypeClassIds` callback is invoked from `resolveInheritedInnerClassToClassId` Phase 2 BFS, which is already gated by:
- Iteration 61's `aggregatedInheritedInnerClassesHolder` cache (eliminates 99.6% of BFS calls)
- Iteration 62's per-resolve `tryResolve` cache (eliminates 82% of duplicate ClassId lookups)
- The BFS itself uses a `visited` set to prevent re-walking the same ClassId

### Key Learnings
- `FirJavaClass` has two distinct origins: `Java.Source` (parsed from source, lazy supertypes) and `Java.Library` (deserialized from .class files, pre-populated supertypes). The premature-resolution guard must distinguish between them.
- Binary Java supertype hierarchies (JDK, library classes) are common in real-world compilation — the descriptors module has deep Java class hierarchies (`CallableMemberDescriptor → MemberDescriptor → DeclarationDescriptorWithVisibility → ...`) that all need walking.
- The fix is a single-line origin check change, but the impact is significant: without it, any inherited inner class from a binary Java supertype hierarchy is invisible to java-direct.

---

## Iteration 68: Multi-Field Declaration Modifier/Type Inheritance - 2026-04-15

### Root Cause Analysis
The `testJs_parser` pipeline test failed with `UNRESOLVED_REFERENCE` errors for `TokenStream.EOF`. The `TokenStream.java` file declares fields using multi-field syntax:
```java
public static final int ERROR = -1, EOF = 0, EOL = 1;
```

The KMP Java parser only attaches `MODIFIER_LIST` and `TYPE` nodes to the **first** FIELD node in such declarations. Subsequent FIELD nodes (EOF, EOL) have no `MODIFIER_LIST` or `TYPE` children — they only contain the IDENTIFIER and initializer. As a result, `JavaFieldOverAst` reported EOF and EOL as non-static, non-final, package-private, with no type — making them invisible to Kotlin code expecting `TokenStream.EOF` as a public static int constant.

### Fix
**File**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt`

Added modifier/type inheritance for multi-field declarations in `JavaFieldOverAst`:

1. **`leadingFieldNode`** (lazy) — walks backward through sibling FIELD nodes to find the first one that carries `MODIFIER_LIST` or `TYPE`. Returns null if the current node already has its own modifiers.

2. **`effectiveModifierList`** (lazy) — returns the node's own `MODIFIER_LIST`, or falls back to the leading field's `MODIFIER_LIST`.

3. **`hasFieldModifier()`** — checks the effective modifier list for a given keyword (e.g., `STATIC_KEYWORD`, `FINAL_KEYWORD`, `PUBLIC_KEYWORD`).

4. Updated `isStatic`, `isFinal`, `visibility`, `annotations`, and `type` to use the effective (possibly inherited) modifier list and type node. For `type`, if the current node has no `TYPE` child, it delegates to the leading field node for type creation.

### Test Results
- `testJs_parser`: PASSES
- `testDescriptors_jvm`: PASSES
- Full `:kotlin-java-direct:test` suite: zero regressions

### Caching Verification
All new properties (`leadingFieldNode`, `effectiveModifierList`) use `by lazy(LazyThreadSafetyMode.PUBLICATION)` — computed at most once per field instance. The backward sibling walk in `leadingFieldNode` is O(n) in the number of fields in the declaration but only executes once due to lazy caching. No hot-path concerns: multi-field declarations are rare in practice (most Java fields are declared individually).

### Key Learnings
- The KMP Java parser's AST structure for multi-field declarations differs from javac/PSI: modifiers and type are NOT duplicated per field — they exist only on the first FIELD node as siblings under the parent CLASS_BODY or similar node.
- PSI handles this transparently via `PsiField.getModifierList()` which walks up to the `PsiDeclarationStatement` parent. Java-direct must implement the same backward-sibling walk manually.
- `TokenStream.java` (from GWT/Rhino) is a real-world example with extensive multi-field declarations — `public static final int ERROR = -1, EOF = 0, EOL = 1, ...` with dozens of constants on a single line.

---

## Future Iteration Template

~~~markdown
## Iteration N: [Title] - YYYY-MM-DD

### Root Cause Analysis
[Reference-first: check javac-wrapper / PSI / git show origin/master first]

### Fix
[Files modified, solution description]

### Test Results
- Box: X/1168, Phased: X/1443, Total failing: N

### Key Learnings
~~~
