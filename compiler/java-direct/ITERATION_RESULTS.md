# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-03-27 (iter 60)

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

The test backend (`AbstractJvmIrBackendFacade.transform`) compiles Java sources with in-process javac via `ToolProvider.getSystemJavaCompiler()`. In the **java-direct test worker JVM**, javac enforces the module seal: `error: package exists in another module: java.base`. In the **PSI test worker JVM**, the same in-process javac with identical options and files succeeds (`result=Success`).

**Investigation verified:**
- Both test workers use `jdkHome=null` (system compiler), identical javac options, identical source files
- The compilation output directory contents are identical (`[META-INF]`)
- The `JavaCompilerFacade.compileJavaFiles` code path is identical — the difference is purely environmental between Gradle test worker JVMs
- The javac failure in `BackendCliJvmFacade.transform` throws `JavaCompilationError` → `ErrorFromFacade` → `processModule(library)` returns false → main module never compiled → diagnostic mismatch + missing JVM artifact cascade

**Also confirmed** that problems #1, #3, #4 do NOT follow this pattern — they fail with genuine FIR diagnostic mismatches (no `JavaCompilationError`).

### Conclusion
Both tests are **won't fix** — they test edge cases where Java sources shadow JDK classes, which is invalid on Java 9+. The PSI test worker's acceptance of this compilation is an environmental coincidence, not a feature java-direct needs to replicate.

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

### Fix (2 files)
1. **`build.gradle.kts`** — Added `withThirdPartyJava8Annotations()` to the `projectTests` block, which sets the `KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH` system property to the absolute path `${project.rootDir}/third-party/java8-annotations`.

2. **`components.kt`** (test fixtures) — Added `registerCompilerExtensions` override to `JavaDirectConfigurator` as defense-in-depth. For CLI-based facades, extensions can be registered via this method in addition to the `COMPILER_PLUGIN_REGISTRARS` mechanism.

### Test Results
- Box: 1168/1168, Phased: 1453/1456, Total failing: 3 (down from 4)
- `testJSpecifySimple` (#3): **FIXED**
- `testJSpecifyWithVarargs` (#4): Still failing — nullability annotations not applied to varargs parameters

### Status of Remaining 3 Failures
| Test | Category | Status |
|------|----------|--------|
| #2 testClassFromJdkInLibrary | javac sealed package | Won't fix |
| #4 testJSpecifyWithVarargs | Varargs nullability enhancement | Remaining |
| #8 testPseudoRawTypes | javac sealed package | Won't fix |

### Key Learnings
- Test infrastructure configuration (`build.gradle.kts` `projectTests` block) must declare ALL third-party annotation dependencies used by tests with `ENABLE_FOREIGN_ANNOTATIONS`
- The `NoSuchFileException` from `JvmForeignAnnotationsConfigurator` was masked by the test runner's multi-failure reporting — always check ALL failure causes, not just the first

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
