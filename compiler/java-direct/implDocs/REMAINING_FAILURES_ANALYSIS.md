# Remaining 8 Test Failures — Detailed Analysis

**Date**: 2026-03-24
**Status**: 2616/2624 passing (8 failing, all in Phased tests)
**Methodology**: Each test was run individually with file-based logging injected into `GlobalMetadataInfoHandler.compareAllMetaDataInfos` to capture actual vs expected FIR dump output. Diffs were then analyzed against the implementation code.

---

## 1. testInheritFromAnnotationClass2

**Category**: Annotation class detection through Java interface hierarchy
**Error type**: Extra diagnostics
**Expected file**: `compiler/testData/diagnostics/tests/annotations/inheritFromAnnotationClass2.fir.kt`

### What the test does

A Java interface `J` extends `kotlin.annotation.Target` (a Kotlin annotation class). Kotlin code then declares:
- `annotation class Ann : Target()` — should get `EXTENDING_AN_ANNOTATION_CLASS_ERROR`
- `annotation class Ann2(...) : Target()` — same
- `interface I : J` — should NOT get the error
- `class C : I` — should NOT get the error
- `annotation class Ann3 : C()` — should NOT get the error
- `annotation class Ann4 : I` — should NOT get the error

### Actual diff

```
Expected: EXTENDING_AN_ANNOTATION_CLASS_ERROR only on Ann and Ann2
Actual:   EXTENDING_AN_ANNOTATION_CLASS_ERROR also on I, C, Ann3, Ann4
```

### Why this happens — compilation pipeline analysis

The diagnostic is produced by `FirAnnotationClassInheritanceChecker` (`compiler/fir/checkers/src/.../FirAnnotationClassInheritanceChecker.kt`). This checker walks the **entire supertype hierarchy** (`deep = true`) of every class declaration using `forEachSupertypeWithInheritor`. When it finds any supertype with `classKind == ClassKind.ANNOTATION_CLASS`, it reports `EXTENDING_AN_ANNOTATION_CLASS_ERROR`.

For `interface I : J`, the checker walks:
1. `I` → `J` (direct supertype)
2. `J` → `kotlin.annotation.Target` (J's supertype from Java `extends Target`)

`kotlin.annotation.Target` has `classKind == ANNOTATION_CLASS`, so the checker fires the error on `I`.

**With PSI**, this error does NOT fire. The most likely explanation: PSI either does not resolve `J`'s supertype `Target` (leaving it as an error/unresolved type), or PSI represents the Java interface `J extends Target` differently such that the FIR supertype walker cannot traverse it. The test suppresses `-MISSING_DEPENDENCY_SUPERCLASS` which suggests PSI produces that diagnostic for `J`'s supertype — meaning PSI's representation of `J` has an unresolvable supertype that the checker can't walk through.

**With java-direct**, `J extends Target` is correctly resolved because java-direct parses the import `import kotlin.annotation.Target` and resolves it via the resolution context. The supertype reference becomes a properly resolved `JavaClassifierType` pointing to `kotlin.annotation.Target`. FIR then creates a fully resolved supertype chain for `J`, allowing the checker to walk through it.

In short: **java-direct resolves J's supertype MORE correctly than PSI does**, which paradoxically causes extra diagnostics.

### Approach to fix

**Option A — Suppress supertype resolution for Java interfaces extending Kotlin annotation classes**: In `JavaClassOverAst.supertypes`, detect when a supertype resolves to a Kotlin annotation class and either omit it or mark it as unresolvable. This would match PSI behavior but feels like a hack.

**Option B — Adjust how java-direct exposes J's supertypes**: When the supertype of a Java interface is a Kotlin annotation class, produce the supertype reference in a way that FIR treats as "unresolvable" (like PSI does). This could be done by NOT resolving the `classifierQualifiedName` for annotation class supertypes, or by returning `classifier = null` with a non-resolvable qualified name.

**Option C — Investigate PSI's exact behavior**: Run the PSI version of this test with debug logging to see exactly what PSI returns for `J.supertypes`. Then replicate that behavior in java-direct. This is the safest approach.

**Recommended**: Option C first (understand PSI), then Option B (targeted fix).

---

## 2. testClassFromJdkInLibrary

**Category**: JDK class classpath precedence
**Error type**: Missing diagnostic
**Expected file**: `compiler/testData/diagnostics/jvmIntegration/classpath/classFromJdkInLibrary.kt` (FIR_IDENTICAL)

### What the test does

A "library" module provides a custom `java.util.Date` class with an extra method `methodWhichDoesNotExistInJdk()`. The main module imports `java.util.Date` and calls that method. The expected behavior is that the main module uses the **JDK's** `java.util.Date` (not the library's), so `methodWhichDoesNotExistInJdk()` should produce `UNRESOLVED_REFERENCE`.

### Actual diff

```
Expected: Date.<!UNRESOLVED_REFERENCE!>methodWhichDoesNotExistInJdk<!>()
Actual:   Date.methodWhichDoesNotExistInJdk()
```

### Why this happens — compilation pipeline analysis

The test uses the `JvmIntegration > Classpath` test category, which involves multi-module compilation with separate classpath configurations. The library module's custom `java.util.Date` is on the classpath.

Java-direct's `JavaClassFinderOverAstImpl` indexes Java source files from provided `sourceRoots`. If the test infrastructure provides the library's source root to java-direct, then java-direct sees the library's `java.util.Date` and resolves it as a Java source class. The JDK's `java.util.Date` is a compiled class (not a source), so it would be resolved by a different class finder (the classpath-based one).

When both exist, the question is which class finder wins. In the PSI pipeline, the JDK class takes precedence over library-provided source classes with the same FQN. In java-direct, the source-based class finder might take precedence, or the classpath order might differ.

This is fundamentally a **test infrastructure / class finder ordering issue**, not a java-direct parsing issue.

### Approach to fix

**Option A — Adjust class finder priority**: When `JavaClassFinderOverAstImpl` finds a class that also exists on the JDK classpath (packages starting with `java.` or `javax.`), prefer the JDK version. This could be a simple filter in `findClass`.

**Option B — Test infrastructure fix**: Check how the test configures source roots. The library module's sources should probably not be passed as source roots to java-direct for the main module's compilation. This might be a test runner configuration issue specific to java-direct tests.

**Option C — Investigate how PSI handles this**: PSI might have explicit JDK-priority logic in its class finder. Check `JavaClassFinderImpl` or the `JavaModuleResolver`.

**Recommended**: Option B first (likely a test configuration issue), then Option A as fallback.

---

## 3. testJSpecifySimple

**Category**: JSpecify foreign annotation processing
**Error type**: Missing diagnostics
**Expected file**: `compiler/fir/analysis-tests/testData/resolve/jSpecifySimple.fir.kt`

### What the test does

Tests JSpecify nullability annotations (`@NonNull`, `@Nullable`, `@NullMarked`) on Java classes. Uses directives `FULL_JDK`, `WITH_STDLIB`, `ENABLE_FOREIGN_ANNOTATIONS`.

Two Java classes:
- `Annotated` with `@Nullable` return and `@NonNull` parameter
- `NullMarkedAnnotated` with class-level `@NullMarked`

Kotlin code calls these and should get `INITIALIZER_TYPE_MISMATCH` and `NULL_FOR_NONNULL_TYPE` diagnostics.

### Actual diff

```
Expected: val x: String <!INITIALIZER_TYPE_MISMATCH!>=<!> Annotated.returnNullable()
          Annotated.takeNonNull(<!NULL_FOR_NONNULL_TYPE!>null<!>)
          NullMarkedAnnotated.takeNonNull(<!NULL_FOR_NONNULL_TYPE!>null<!>)

Actual:   val x: String = Annotated.returnNullable()
          Annotated.takeNonNull(null)
          NullMarkedAnnotated.takeNonNull(null)
```

No nullability diagnostics are produced at all.

### Why this happens — compilation pipeline analysis

FIR's nullability enhancement pipeline reads annotations from Java class members to determine nullability. For JSpecify annotations, this requires:

1. The Java class's annotations are parsed correctly by java-direct (method/parameter level `@NonNull`, `@Nullable`)
2. The class-level `@NullMarked` annotation is parsed
3. The `ENABLE_FOREIGN_ANNOTATIONS` directive causes FIR to treat JSpecify annotations as nullability markers
4. FIR's Java enhancement (`FirAnnotationEnhancement`) reads these annotations and adjusts type nullability

The JSpecify annotations (`org.jspecify.annotations.NonNull`, etc.) are external library annotations, not part of the JDK or standard library. They must be on the classpath.

The most likely root cause is that **the JSpecify annotation JAR is not on the classpath** when running java-direct tests, OR the test infrastructure doesn't configure `ENABLE_FOREIGN_ANNOTATIONS` correctly for java-direct test runners. If the annotations can't be resolved, FIR can't determine they're nullability annotations, and no diagnostics are produced.

Another possibility: java-direct correctly parses the annotations but doesn't resolve their ClassIds correctly for FIR's annotation enhancement to recognize them.

### Approach to fix

**Step 1 — Verify annotation resolution**: Add debug logging to check if java-direct produces annotations with correct ClassIds for JSpecify annotations. Run the test and check if `JavaAnnotationOverAst.classId` returns `org.jspecify.annotations/NonNull`.

**Step 2 — Check test infrastructure**: Compare the test runner configuration for java-direct (`JavaUsingAstPhasedTestGenerated`) vs PSI (`PhasedJvmDiagnosticLightTreeTestGenerated`) — specifically the `ENABLE_FOREIGN_ANNOTATIONS` directive handling and classpath setup.

**Step 3 — Check annotation resolution pipeline**: Trace through `FirAnnotationEnhancement` to see if the java-direct annotations reach it. The issue might be in how `JavaAnnotationOverAst.resolveAnnotation` handles external annotation ClassIds.

**Recommended**: Step 2 first — this is likely a test infrastructure / classpath issue.

---

## 4. testJSpecifyWithVarargs

**Category**: JSpecify foreign annotation processing (same root cause as #3)
**Error type**: Missing diagnostics
**Expected file**: `compiler/testData/diagnostics/tests/j+k/jSpecifyWithVarargs.fir.kt`

### What the test does

Tests `@NonNull` from both JSpecify and JetBrains on vararg parameters. Both should produce `NULL_FOR_NONNULL_TYPE`.

### Actual diff

```
Expected: JavaClass.ofJetbrains(<!NULL_FOR_NONNULL_TYPE!>null<!>)
          JavaClass.ofJspecify(<!NULL_FOR_NONNULL_TYPE!>null<!>)

Actual:   JavaClass.ofJetbrains(null)
          JavaClass.ofJspecify(null)
```

Neither JetBrains `@NotNull` nor JSpecify `@NonNull` produces diagnostics.

### Why this happens

Same root cause as test #3. The fact that even JetBrains `@NotNull` doesn't work (not just JSpecify) suggests the issue is broader — possibly the `ENABLE_FOREIGN_ANNOTATIONS` directive or the annotation JAR classpath configuration is not set up for java-direct tests.

### Approach to fix

Same as test #3. Fix one and the other should follow.

---

## 5. testKJKComplexHierarchyWithNested

**Category**: K-J-K hierarchy with parameterized nested classes
**Error type**: Extra diagnostics (`MISSING_DEPENDENCY_SUPERCLASS`, `UNRESOLVED_REFERENCE`)
**Expected file**: `compiler/fir/analysis-tests/testData/resolveWithStdlib/j+k/KJKComplexHierarchyWithNested.fir.txt`

### What the test does

A complex Kotlin→Java→Kotlin hierarchy:
- `KSub` (Kotlin) extends `J1` (Java) extends `KFirst` (Kotlin) extends `SuperClass<String>`, `SuperI<Int>`
- `J1` has nested classes `NestedSubClass extends NestedInSuperClass` and `NestedIImpl implements NestedInI<NestedInSuperClass>`
- Tests type argument propagation through the chain

### Actual diff

The expected output is a FIR tree dump showing resolved types. The actual output contains source code with `MISSING_DEPENDENCY_SUPERCLASS` and `UNRESOLVED_REFERENCE` diagnostics on nested class method calls:
```
k.getImpl().<!MISSING_DEPENDENCY_SUPERCLASS, UNRESOLVED_REFERENCE!>nestedI<!>(vString)
k.getNestedSubClass().<!MISSING_DEPENDENCY_SUPERCLASS, UNRESOLVED_REFERENCE!>nested<!>("")
```

### Why this happens — compilation pipeline analysis

The Java class `J1` declares:
```java
public class J1 extends KFirst {
    public class NestedSubClass extends NestedInSuperClass {}
    public abstract class NestedIImpl implements NestedInI<NestedInSuperClass> {}
}
```

`NestedInSuperClass` and `NestedInI` are inner/nested classes of `SuperClass<T>` and `SuperI<E>` respectively (Kotlin classes). When java-direct parses `J1`, it needs to resolve:
1. `NestedInSuperClass` → `SuperClass.NestedInSuperClass` (inherited from `KFirst extends SuperClass<String>`)
2. `NestedInI<NestedInSuperClass>` → `SuperI.NestedInI<SuperClass.NestedInSuperClass>` (inherited from `KFirst extends SuperI<Int>`)

This resolution requires walking through the K-J-K supertype chain: `J1 → KFirst → SuperClass/SuperI`. Since `KFirst` is a Kotlin class, java-direct's `resolveInheritedInnerClassToClassId` cannot walk through it (it can only resolve direct supertypes, not their supertypes through Kotlin classes).

Additionally, the **parameterized type arguments** need to be threaded correctly through the chain, which adds another layer of complexity.

### Approach to fix

This test requires **cross-language supertype walking** — the same fundamental issue as test #7 but with additional complexity from parameterized inner classes.

**Option A — FIR-side supertype callback (requires careful deadlock avoidance)**: Add a `getDirectSupertypeClassIds` callback to the resolution pipeline. The callback must NOT trigger `lazyResolveToPhase(SUPER_TYPES)` — it should only read already-resolved supertype refs from `fir.superTypeRefs`. Investigation showed that even `fir.superTypeRefs` access caused hangs (likely due to compilation ordering rather than true deadlocks). A safe implementation would need to:
- Only read `FirResolvedTypeRef` entries (skip unresolved ones)
- Add a recursion guard to prevent cycles
- Only activate for java-direct types (PSI types would skip it)

**Option B — Two-phase resolution**: In the first phase, java-direct returns unresolved types. In a later phase (after all supertypes are resolved), re-resolve the unresolved types using FIR's now-complete supertype information.

**Recommended**: This is the hardest of all 8 tests. Tackle after tests #7 and #6 are resolved, as lessons learned there will apply.

---

## 6. testMapMethodsImplementedInJava

**Category**: Abstract member detection through generic Java hierarchy
**Error type**: Extra diagnostic (`ABSTRACT_MEMBER_NOT_IMPLEMENTED`)
**Expected file**: `compiler/testData/diagnostics/tests/j+k/collectionOverrides/mapMethodsImplementedInJava.kt` (FIR_IDENTICAL)

### What the test does

```java
abstract class Base<T> implements Map<String, T> {
    @Override public abstract String get(Object key);
}
class Derived extends Base<String> {
    // Implements ALL 13+ Map methods
}
```
```kotlin
class Impl : Derived()  // Should NOT get ABSTRACT_MEMBER_NOT_IMPLEMENTED
```

The test uses `DISABLE_JAVA_FACADE` and `FULL_JDK`.

### Actual diff

```
Expected: class Impl : Derived()
Actual:   <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Impl<!> : Derived()
```

### Why this happens — compilation pipeline analysis

FIR checks whether all abstract methods from supertypes are implemented. For `Impl : Derived()`, FIR needs to see that `Derived` implements all `Map` methods.

The `DISABLE_JAVA_FACADE` directive is key. Investigating the test infrastructure:
- `FirDiagnosticsDirectives.DISABLE_JAVA_FACADE` (line 118 in `FirDiagnosticsDirectives.kt`)
- Used in `JvmIrBackendFacade.kt` to skip Java source compilation in the backend

In the frontend phase, FIR processes `Derived` as a Java class. `Derived extends Base<String>` and `Base<T> implements Map<String, T>`. FIR needs to see:
1. `Derived` provides concrete implementations for `get`, `size`, `isEmpty`, etc.
2. `Base<T>` declares `abstract get(Object)` and inherits abstract methods from `Map`
3. The type substitution `T=String` propagates correctly

The most likely issue is that `DISABLE_JAVA_FACADE` affects how Java source files are processed. With PSI, disabling the Java facade might still allow PSI to provide full class information. With java-direct, it might disable the java-direct class finder or change the resolution path, causing `Derived`'s methods to not be visible.

Another possibility: `Base` is an abstract class with `implements Map<String, T>`. If java-direct doesn't correctly represent `Base`'s supertypes (specifically the `Map<String, T>` with the type parameter), FIR might not correctly compute the set of abstract methods that need implementation.

### Approach to fix

**Step 1 — Check DISABLE_JAVA_FACADE handling**: Trace through the test infrastructure to understand what `DISABLE_JAVA_FACADE` does in the java-direct test runner. Check if it disables the class finder or changes the resolution pipeline.

**Step 2 — Debug method visibility**: Add exception debugging to check if FIR sees all 13+ methods in `Derived`. If methods are missing, the issue is in how java-direct parses `Derived`.

**Step 3 — Check generic supertype representation**: Verify that java-direct correctly represents `Base<T> implements Map<String, T>` with the type parameter `T` in the `Map` type arguments.

**Recommended**: Step 1 first — this might be purely a test infrastructure issue.

---

## 7. testInheritanceWithKotlin

**Category**: Inherited nested class resolution through Java→Kotlin→Kotlin→Java chain
**Error type**: Extra diagnostics (`MISSING_DEPENDENCY_CLASS`)
**Expected file**: `compiler/testData/diagnostics/tests/javac/inheritance/InheritanceWithKotlin.kt` (FIR_IDENTICAL)

### What the test does

```java
// UseKotlinInner.java
public class UseKotlinInner extends KotlinClass {
    KotlinInner getKotlinInner() { return null; }
    JavaInner getJavaInner() { return null; }
    KotlinInner3 getKotlinInner3() { return null; }
}
```

Inheritance chain:
- `UseKotlinInner` → `KotlinClass` → `KotlinInterface.KotlinInner2` → `JavaClass2`
- `KotlinInner` is inner class of `KotlinClass` (1 level)
- `JavaInner` is static nested class of `JavaClass2` (3 levels through Kotlin)
- `KotlinInner3` is nested class of `KotlinInner2` (2 levels through Kotlin)

### Actual diff

```
Expected: private fun getJavaInner() = UseKotlinInner().javaInner
          private fun getKotlinInner3() = UseKotlinInner().kotlinInner3

Actual:   private fun getJavaInner() = UseKotlinInner().<!MISSING_DEPENDENCY_CLASS!>javaInner<!>
          private fun getKotlinInner3() = UseKotlinInner().<!MISSING_DEPENDENCY_CLASS!>kotlinInner3<!>
```

Note: `getKotlinInner()` (1 level deep) works correctly. Only deeper inheritance fails.

### Why this happens — compilation pipeline analysis

When java-direct parses `UseKotlinInner.java`, the return type `JavaInner` in method `getJavaInner()` needs to be resolved. The resolution flow in `JavaResolutionContext`:

1. **`resolveSimpleNameToClassId("JavaInner", tryResolve)`** is called
2. **Import check**: No import for `JavaInner` → skip
3. **`findLocalClass("JavaInner")`**: Checks inner classes of `UseKotlinInner` (none), supertypes (local only, `KotlinClass` is not in same Java file) → null
4. **Cross-file inherited inner class check via `collectInheritedInnerClasses`**: This calls `getDirectSupertypes(test/UseKotlinInner)` which calls `resolveSupertypeReference("KotlinClass", test)`. This method (`JavaClassFinderOverAstImpl:384`) only checks the **Java source index**: `index[packageFqName]?.containsKey(simpleName)`. Since `KotlinClass` is a Kotlin class, it's NOT in the Java source index → returns null → `getDirectSupertypes` returns empty list → no inheritance walking occurs.
5. **`resolveInheritedInnerClassToClassId("JavaInner", tryResolve)`**: Resolves direct supertype `KotlinClass` → `ClassId(test, KotlinClass)` via `tryResolve` callback (FIR knows about it). Tries `ClassId(test, KotlinClass.JavaInner)` → `tryResolve` returns false (KotlinClass doesn't have a direct nested class called JavaInner). **Gives up — only checks one level deep.**
6. **Same-package check**: `ClassId(test, JavaInner)` → no top-level class named `JavaInner` → fails
7. **Resolution returns null** → type becomes unresolved → `MISSING_DEPENDENCY_CLASS`

`KotlinInner` works (step 5) because it IS a direct inner class of `KotlinClass`: `ClassId(test, KotlinClass.KotlinInner)` resolves successfully via `tryResolve`.

`JavaInner` fails because its actual ClassId is `test/JavaClass2.JavaInner`, which is 3 levels up: `UseKotlinInner → KotlinClass → KotlinInner2 → JavaClass2`. The resolution would need to walk through `KotlinClass`'s supertypes, then `KotlinInner2`'s supertypes, to find `JavaClass2`.

### Approach to fix (attempted and findings)

**Attempt 1 — FIR supertype callback**: Added a `getDirectSupertypeClassIds: ((ClassId) -> List<ClassId>)?` parameter to the `resolve` method on `JavaClassifierType`. The callback in `JavaTypeConversion.kt` queried `FirRegularClassSymbol.resolvedSuperTypeRefs` to get supertypes. **Result: test hung (deadlock/infinite wait)**. Even using `fir.superTypeRefs` directly (without `lazyResolveToPhase`) caused hangs, likely because accessing FIR class data during Java type conversion triggers resolution cycles.

**Attempt 2 — Same-package probe fallback**: Added a fallback in `resolveInheritedInnerClassToClassId` that probes all known same-package Java source classes for a matching nested class using `tryResolve`. **Result: partially works** — found `JavaClass2.JavaInner` but couldn't find `KotlinInner3` because it's nested inside `KotlinInterface.KotlinInner2` (a Kotlin class, not in the Java source index, and nested 2 levels deep).

**Safe FIR callback approach**: The key challenge is that `resolvedSuperTypeRefs` triggers lazy resolution. A safe approach would need to:
1. Check if supertypes are already resolved (`fir.superTypeRefs.all { it is FirResolvedTypeRef }`) before accessing them
2. If not yet resolved, skip the deep walk (accept the limitation)
3. Add a recursion depth limit
4. Only activate for java-direct types (check `javaType is JavaClassifierTypeOverAst`)

The hang during Attempt 1 might also be caused by the Kotlin daemon's incremental compilation state getting confused. A clean build should be tried.

**Recommended**: Retry the FIR callback approach with safeguards (resolution check, depth limit, clean build). If that still hangs, implement a two-phase approach: first pass resolves what it can, second pass (after SUPER_TYPES phase completes) re-resolves remaining unresolved types.

---

## 8. testPseudoRawTypes

**Category**: Java compilation failure with shadowed JDK class
**Error type**: `JavaCompilationError` (not FIR dump mismatch)
**Expected file**: `compiler/testData/diagnostics/tests/generics/PseudoRawTypes.kt`

### What the test does

Defines a custom `java.util.Collection` class (package `java.util`) without generics, simulating a "pseudo-raw type". A `Usage` Java class uses this fake `Collection`. Kotlin code calls methods on it.

The test has `JAVAC_EXPECTED_FILE`, `FIR_IDENTICAL`, `RUN_PIPELINE_TILL: BACKEND`.

### Actual error

```
org.jetbrains.kotlin.test.JavaCompilationError
```

This is a completely different error type — the Java sources failed to compile, not a FIR dump mismatch.

### Why this happens — compilation pipeline analysis

The test declares a class in package `java.util`, which conflicts with the JDK's `java.util` package. When the test infrastructure compiles the Java sources, the Java compiler (`javac`) may refuse to compile a class in a JDK package due to module system restrictions (Java 9+).

With PSI, the test infrastructure may use a different Java compilation approach (e.g., PSI's own Java parser which doesn't invoke `javac`) or configure the compilation to allow JDK package shadowing.

With java-direct, the test infrastructure likely invokes `javac` to compile the Java sources (needed for the `RUN_PIPELINE_TILL: BACKEND` directive), and `javac` rejects the `java.util.Collection` class.

### Approach to fix

**Option A — Test infrastructure configuration**: Configure the Java compilation step for java-direct tests to allow JDK package shadowing (e.g., `--patch-module` or disabling module restrictions).

**Option B — Skip test**: If this is a fundamental limitation of the test setup (java-direct tests compile Java sources differently), this test could be muted or skipped for java-direct with documentation.

**Option C — Investigate PSI test runner**: Check how the PSI test runner handles this test — specifically whether it actually compiles the Java sources with `javac` or uses PSI parsing only.

**Recommended**: Option C first, then Option A.

---

## Priority Ranking

Based on fix complexity, impact, and likelihood of success:

| Priority | Tests | Why |
|----------|-------|-----|
| 1 | #3 + #4 (JSpecify) | Likely a test infrastructure / classpath issue — smallest code change, fixes 2 tests |
| 2 | #6 (MapMethods) | Likely a DISABLE_JAVA_FACADE handling issue — investigate test infra first |
| 3 | #2 (ClassFromJdk) | Classpath precedence — well-defined scope |
| 4 | #8 (PseudoRawTypes) | Java compilation config — may be a simple flag |
| 5 | #1 (AnnotationClass) | Requires understanding PSI's exact behavior for annotation supertypes |
| 6 | #7 (InheritanceWithKotlin) | Requires FIR supertype callback with deadlock avoidance |
| 7 | #5 (KJKComplex) | Hardest — requires both supertype walking AND parameterized type propagation |
