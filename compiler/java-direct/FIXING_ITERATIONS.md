# Java-Direct: Fixing Iterations

## Document Purpose

This document contains structured iteration prompts for fixing issues in the `java-direct` module. Each iteration should be executed sequentially after confirming the previous iteration is complete.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Usage**: Execute one iteration at a time, following the 4-phase template  
**Status**: Iteration 7c complete (101/138 = 73.2%), Iteration 8 ready  
**Last Updated**: 2026-03-04  
**Planning Changelog**: See `implDocs/PLANNING_CHANGELOG.md` for iteration restructuring history

---

## How to Use This Document

1. **Before starting**: Read `AGENT_INSTRUCTIONS.md` thoroughly
2. **For each iteration**:
   - Reference `AGENT_INSTRUCTIONS.md` for common guidelines
   - Read the iteration prompt below
   - Follow the 4-phase template (Analysis → Reproduction → Implementation → Validation)
   - Ask for confirmation before proceeding to implementation
   - Report results after completion
   - **MANDATORY**: Update `ITERATION_RESULTS.md` with findings (use template provided)
3. **Between iterations**: Get user confirmation to proceed to next iteration

## Capturing Learnings

**IMPORTANT**: After completing each iteration, you MUST append your results to `ITERATION_RESULTS.md`.

This file captures:
- Key findings about the codebase
- Implementation decisions and trade-offs
- Test results and improvements
- Issues encountered and solutions
- Recommendations for future work

See the template in `ITERATION_RESULTS.md` for the required format. This ensures knowledge is preserved across iterations without overloading context.

---

## Iterations 1-6: Completed (Archived)

**Status**: ✅ All completed  
**Final Result**: 90/138 (65.2%) box tests passing  
**Archive**: See `implDocs/archive/ITERATIONS_1_6_DETAILS.md` for full details

### Summary of Completed Iterations

| Iteration | Focus | Key Result | Tests |
|-----------|-------|------------|-------|
| 1 | Initial Root Cause Analysis | Fixed `hasDefaultConstructor()` returning false | 0→1/138 |
| 2 | Type Resolution Architecture | Verified classifierQualifiedName approach works | 1/138 |
| 3 | Import Handling | Implemented JavaImports, simple import qualification | 1→11/138 |
| 4 | Star Import Resolution + Parameters | Callback approach + parameter parsing | 11→30/138 |
| 5 | Type Arguments Parsing | Implemented generic type arguments + visibility fix | 30→31/138 |
| 6 | Hybrid JavaClassFinder | Combined source+binary class finding | 31→90/138 |

### Key Architectural Decisions Made (Iterations 1-6)

1. **Type Resolution in FIR Layer**: Java Model provides names (`classifierQualifiedName`), FIR provides resolution. No `FirSession` access in Java Model.

2. **Callback Pattern for Star Imports**: `resolve(tryResolve: (String) -> Boolean)` allows Java Model to implement Java resolution rules while FIR validates existence.

3. **Hybrid Finder Architecture**: Source-first, binary-fallback pattern allows java-direct for sources while maintaining platform infrastructure for binaries.

> ⚠️ **Deep Context Recovery**: Only consult `implDocs/archive/ITERATIONS_1_6_DETAILS.md` and `implDocs/archive/DESIGN_DOCUMENTS_ARCHIVE.md` if you need to understand historical decisions or debug regressions. For ongoing work, the summaries above should suffice.

---

## Iteration 7: Array Types and Type Parameter Bounds

**Status**: Analysis complete, ready for implementation  
**Analysis Document**: `implDocs/ITERATION_7_PROBLEM_ANALYSIS.md`  
**Expected Improvement**: 5-10 tests (array types alone: 5 tests)

### Background

After iteration 6, systematic analysis of 48 remaining failures revealed several root causes:

| Category | Count | This Iteration? |
|----------|-------|-----------------|
| Array types not parsed | 5 | **Yes - Quick Win** |
| Type parameter bounds | 7+ | **Yes** |
| Kotlin classes from Java | 15 | Partial investigation |
| Other issues | 21 | Deferred |

### Prompt

---
TASK: Fix array type parsing and implement type parameter bounds

CONTEXT:
**Current Status: 90/138 (65.2%) box tests passing**

Analysis revealed two concrete issues with known fixes:

1. **Array types (`String[]`) parsed as `String`** (5 tests)
   - AST structure discovered via exception-based debugging
   - Fix location identified: `createJavaType()` in `JavaTypeOverAst.kt`

2. **Type parameter bounds not parsed** (7+ tests)
   - `JavaTypeParameterOverAst.upperBounds` returns empty list
   - `JavaMethodOverAst.typeParameters` returns empty list

IMPLEMENTATION STEPS:

### Phase 1: Array Type Parsing (5 tests)

1. **Understand the AST structure** (already discovered):
   ```
   TYPE: String[]
     TYPE: String           <- component type
       JAVA_CODE_REFERENCE: String
     LBRACKET: [
     RBRACKET: ]
   ```

2. **Modify `createJavaType()` in `JavaTypeOverAst.kt`**:
   ```kotlin
   fun createJavaType(node: JavaSyntaxNode, ...): JavaType {
       val typeNode = node.findChildByType("TYPE") ?: node
       
       // NEW: Check for array type (has LBRACKET child)
       if (typeNode.findChildByType("LBRACKET") != null) {
           val componentTypeNode = typeNode.findChildByType("TYPE")
               ?: return JavaClassifierTypeOverAst(typeNode, source, localScope, imports)
           val componentType = createJavaType(componentTypeNode, source, localScope, imports)
           return JavaArrayTypeOverAst(typeNode, source, componentType)
       }
       
       // Existing primitive/reference handling...
   }
   ```

3. **Handle multi-dimensional arrays** (`int[][]`):
   - Recursively apply array detection
   - Each dimension adds another `LBRACKET`/`RBRACKET` pair

4. **Verify with test**:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testOverrideWithArrayParameterType" -q
   ```

### Phase 2: Type Parameter Bounds (7+ tests)

5. **Update `JavaTypeParameterOverAst.upperBounds`**:
   ```kotlin
   override val upperBounds: Collection<JavaClassifierType>
       get() {
           val extendsList = node.findChildByType("EXTENDS_BOUND_LIST")
               ?: return emptyList()
           return extendsList.getChildrenByType("JAVA_CODE_REFERENCE")
               .map { JavaClassifierTypeOverAst(it, source) }
       }
   ```

6. **Update `JavaMethodOverAst.typeParameters`**:
   ```kotlin
   override val typeParameters: List<JavaTypeParameter>
       get() = node.findChildByType("TYPE_PARAMETER_LIST")
           ?.getChildrenByType("TYPE_PARAMETER")
           ?.map { JavaTypeParameterOverAst(it, source) }
           ?: emptyList()
   ```

7. **Verify with test**:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testSamTypeParameter" -q
   ```

### Phase 3: Validation

8. **Run all box tests**:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q
   ```

9. **Check for file problems**:
   Use `mcp__jetbrains__get_file_problems` on modified files.

10. **Document results** in `ITERATION_RESULTS.md`.

TARGET: 95-100/138 (69-72%) pass rate

DELIVERABLE:
- Array type parsing working
- Type parameter bounds parsed
- Method-level type parameters parsed
- +5-10 passing tests

---

### Iteration 7c: Type Parameter Scope Resolution

**Status**: Ready for implementation  
**Analysis**: See investigation in `implDocs/INVESTIGATION_TECHNIQUES.md`  
**Expected Improvement**: 30-40 tests (majority of MISSING_DEPENDENCY_CLASS errors)

#### Problem Statement

When java-direct parses a type reference like `T` in Java code:
```java
class Foo<T> {
    T getValue();           // "T" should resolve to the type parameter
    List<T> getItems();     // "T" in generic argument should also resolve
}
```

The current implementation treats `T` as a class name, causing `MISSING_DEPENDENCY_CLASS: Cannot access class 'T'`.

**Error message examples**:
- `Cannot access class 'T'` (35+ tests)
- `Cannot access class 'E'`, `'R'`, `'U'`, `'S'`, `'K'`, `'V'`, `'F'` etc.
- `Cannot access class '?'` (wildcard not handled)

#### Root Cause

`JavaClassifierTypeOverAst.classifier` only checks local classes via `resolutionContext.findLocalClass()`. It does NOT check if the name refers to a **type parameter** of the containing class or method.

**PSI-based implementation** (working):
```java
// JavaClassifierImpl.java:32-38
if (psiClass instanceof PsiTypeParameter) {
    return new JavaTypeParameterImpl(...);  // Returns JavaTypeParameter!
}
```

**Javac-based implementation** (working):
```kotlin
// ClassifierResolver.kt:199-205
enclosingClass.typeParameters
    .find { it.name == identifier }
    ?.let { return it }  // Returns JavaTypeParameter!
```

**Java-direct** (broken): Returns `null`, then FIR uses `classifierQualifiedName = "T"` and tries to look it up as a class.

#### Implementation Plan

##### Phase 1: Add Type Parameter Scope to Resolution Context

1. **Extend `JavaResolutionContext`** to track type parameters in scope:
   ```kotlin
   class JavaResolutionContext private constructor(
       // ... existing fields ...
       private val typeParametersInScope: Map<String, JavaTypeParameter>,
   ) {
       fun findTypeParameter(name: String): JavaTypeParameter? = 
           typeParametersInScope[name]
       
       fun withTypeParameters(
           typeParams: List<JavaTypeParameter>
       ): JavaResolutionContext {
           val newScope = typeParametersInScope + 
               typeParams.associateBy { it.name.asString() }
           return JavaResolutionContext(
               source, packageFqName, simpleImports, starImports,
               localClassProvider, newScope
           )
       }
   }
   ```

2. **Update `JavaResolutionContext.create()`** to initialize with empty type parameter scope.

##### Phase 2: Update Type Creation to Check Type Parameters

3. **Modify `JavaClassifierTypeOverAst.classifier`** to check type parameters first:
   ```kotlin
   override val classifier: JavaClassifier? by lazy {
       val simpleName = rawTypeName.split('.').first()
       
       // 1. Check type parameters in scope FIRST
       resolutionContext.findTypeParameter(simpleName)?.let { return@lazy it }
       
       // 2. Then check local classes (existing logic)
       val parts = rawTypeName.split('.')
       var current: JavaClassifier? = resolutionContext.findLocalClass(Name.identifier(parts[0]))
       // ... rest of existing logic ...
   }
   ```

4. **Update `classifierQualifiedName`** similarly to return the type parameter name if matched.

##### Phase 3: Propagate Type Parameters Through AST

5. **Update `JavaClassOverAst`** to create context with class type parameters:
   ```kotlin
   private val innerContext: JavaResolutionContext by lazy {
       val classTypeParams = typeParameters  // Already parsed
       resolutionContext.withTypeParameters(classTypeParams)
   }
   ```

6. **Update `JavaMethodOverAst`** to add method type parameters:
   ```kotlin
   private val methodContext: JavaResolutionContext by lazy {
       val methodTypeParams = typeParameters  // Method's own type params
       containingClass.innerContext.withTypeParameters(methodTypeParams)
   }
   ```

7. **Pass the correct context** when creating types for:
   - Method return types
   - Method parameter types
   - Field types
   - Type parameter bounds

##### Phase 4: Handle Wildcards

8. **Add wildcard detection** in `createJavaType()`:
   ```kotlin
   fun createJavaType(node: JavaSyntaxNode, resolutionContext: JavaResolutionContext): JavaType {
       // Check for wildcard (unbounded: ?, bounded: ? extends X, ? super X)
       val questionMark = node.findChildByType("QUEST") // or whatever KMP uses
       if (questionMark != null) {
           val extendsKeyword = node.findChildByType("EXTENDS_KEYWORD")
           val superKeyword = node.findChildByType("SUPER_KEYWORD")
           val boundNode = node.findChildByType("TYPE")
           val bound = boundNode?.let { createJavaType(it, resolutionContext) }
           val isExtends = extendsKeyword != null || superKeyword == null
           return JavaWildcardTypeOverAst(node, source, bound as? JavaClassifierType, isExtends)
       }
       // ... existing array/primitive/reference handling ...
   }
   ```

##### Phase 5: Validation

9. **Run targeted tests**:
   ```bash
   # Tests with type parameter errors
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testPropertyVarianceConflict" -q
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testKt42825" -q
   ```

10. **Run full suite**:
    ```bash
    ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated*" -q
    ```

11. **Document results** in `ITERATION_RESULTS.md`.

#### Expected Results

| Metric | Before | After |
|--------|--------|-------|
| Box tests passing | 96/138 (69.6%) | ~120-130/138 (87-94%) |
| MISSING_DEPENDENCY_CLASS errors | 35+ | ~5 (non-type-param issues) |

#### Key Files to Modify

| File | Changes |
|------|---------|
| `JavaResolutionContext.kt` | Add type parameter scope, `withTypeParameters()` method |
| `JavaTypeOverAst.kt` | Update `classifier` to check type parameters first |
| `JavaTypeOverAst.kt` | Add wildcard type handling in `createJavaType()` |
| `JavaClassOverAst.kt` | Create inner context with class type parameters |
| `JavaMemberOverAst.kt` | Create method context with method type parameters |

#### Risks and Considerations

1. **Circular initialization**: Type parameters may reference each other in bounds. Use lazy evaluation.
2. **Nested classes**: Inner classes see outer class type parameters, static nested classes don't.
3. **Wildcard AST structure**: Need to verify KMP parser's representation of wildcards.

---

## Iteration 8: Annotations and Nullability

**Status**: Ready for implementation  
**Expected Improvement**: +8 tests (NPE_ASSERTION failures)  
**Note**: Merged from old Iterations 11 and 13. Old Iterations 8 and 10 (Wildcards, Lower Bounds) were completed in 7c.

### Background

8 tests fail with NPE_ASSERTION errors - nullability checks not triggering when they should:
- `testInFunctionWithExpressionBody`
- `testInLocalFunctionWithExpressionBody`
- `testInLocalVariableInitializer`
- `testInMemberPropertyInitializer`
- `testInPropertyGetterWithExpressionBody`
- `testInTopLevelPropertyInitializer`
- `testNnStringVsTXArray`
- `testNnStringVsTXString`

These tests expect `NullPointerException` to be thrown when null is passed to a non-null parameter, but the assertion is not being generated.

### Prompt

---
TASK: Implement annotation parsing for nullability annotations

CONTEXT:
**Current Status: 101/138 (73.2%) box tests passing**

8 tests fail because nullability annotations (`@NotNull`, `@Nullable`, `@NonNull`) are not being parsed or attached to declarations properly. FIR uses these to generate null-checks.

INVESTIGATION STEPS:

### Phase 1: Understand Current State

1. **Check existing annotation implementation**:
   ```kotlin
   // In JavaClassOverAst, JavaMethodOverAst, JavaFieldOverAst:
   override val annotations: Collection<JavaAnnotation> get() = ???
   ```
   - What does `JavaAnnotationOwner.annotations` currently return?
   - Is `JavaAnnotationOverAst` implemented?

2. **Examine PSI-based annotation handling**:
   - `compiler/frontend.common.jvm/.../impl/JavaAnnotationImpl.kt`
   - How are annotations extracted from PSI?

3. **Check FIR's usage of annotations**:
   - Search for `@NotNull`, `@Nullable` handling in FIR
   - How does FIR decide to generate null-checks?

### Phase 2: Implement Annotation Parsing

4. **Parse annotation AST structure**:
   ```
   MODIFIER_LIST:
     ANNOTATION:
       AT: @
       JAVA_CODE_REFERENCE: NotNull
         IDENTIFIER: NotNull
   ```

5. **Implement `JavaAnnotationOverAst`**:
   ```kotlin
   class JavaAnnotationOverAst(
       node: JavaSyntaxNode,
       resolutionContext: JavaResolutionContext,
   ) : JavaAnnotation {
       override val classId: ClassId? by lazy {
           val ref = node.findChildByType("JAVA_CODE_REFERENCE")
           // Resolve annotation class using imports
       }
       
       override val arguments: Collection<JavaAnnotationArgument>
           get() = emptyList() // Defer complex arguments
   }
   ```

6. **Update annotation owners** to parse MODIFIER_LIST:
   - `JavaClassOverAst.annotations`
   - `JavaMethodOverAst.annotations`
   - `JavaFieldOverAst.annotations`
   - `JavaValueParameterOverAst.annotations`

### Phase 3: Handle Common Nullability Annotations

7. **Support standard annotation packages**:
   - `org.jetbrains.annotations.NotNull`
   - `org.jetbrains.annotations.Nullable`
   - `javax.annotation.Nonnull`
   - `javax.annotation.Nullable`
   - `androidx.annotation.NonNull`
   - `androidx.annotation.Nullable`

8. **Handle TYPE_USE annotations** (Java 8+):
   - Annotations can appear on types: `@NotNull String`
   - Check `JavaType.annotations` as well as declaration annotations

### Phase 4: Validation

9. **Run targeted tests**:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testInFunctionWithExpressionBody" -q
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testNnStringVsTXString" -q
   ```

10. **Run full suite**:
    ```bash
    ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated*" -q
    ```

11. **Document results** in `ITERATION_RESULTS.md`.

TARGET: 109/138 (79%) pass rate (+8 tests)

DELIVERABLE:
- Annotation parsing working
- Nullability annotations attached to declarations
- +8 passing tests

---

## Iteration 9: SAM Conversion and Interface Fields

**Status**: Ready for implementation  
**Expected Improvement**: +5-8 tests

### Background

Several tests fail due to SAM conversion issues and interface field access:
- `UNRESOLVED_REFERENCE` for interface fields (3 tests)
- `CANNOT_INFER_PARAMETER_TYPE` for SAM types (2 tests)
- `ARGUMENT_TYPE_MISMATCH` for SAM/lambda (4 tests)

### Prompt

---
TASK: Fix interface field access and SAM conversion issues

CONTEXT:
**Expected Status After Iteration 8: ~109/138 (79%) tests passing**

INVESTIGATION STEPS:

### Phase 1: Interface Field Access

1. **Understand the problem**:
   - `testJavaInterfaceFieldDirectAccess` fails with UNRESOLVED_REFERENCE
   - Interface constants like `interface J { String CONST = "value"; }` not accessible

2. **Check `JavaClassOverAst.fields`**:
   - Are interface fields being parsed?
   - Are they marked as static/final correctly?

3. **Fix interface field parsing**:
   - Interface fields are implicitly `public static final`
   - Ensure they appear in `fields` collection

### Phase 2: SAM Conversion

4. **Understand SAM failures**:
   - `testGenericSamProjectedOut` - UNRESOLVED_REFERENCE
   - `testSamTypeParameter` - CANNOT_INFER_PARAMETER_TYPE
   - `testJavaNestedSamInterface` - ARGUMENT_TYPE_MISMATCH

5. **Check functional interface detection**:
   - Is `isFunctionalInterface` property correct?
   - Are SAM method signatures correct?

6. **Verify type parameter handling in SAM context**:
   - SAM with generics: `interface Mapper<T, R> { R map(T t); }`
   - Type inference for lambda parameters

### Phase 3: Validation

7. **Run targeted tests**:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testJavaInterfaceFieldDirectAccess" -q
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testSamTypeParameter" -q
   ```

8. **Run full suite** and document.

TARGET: 115/138 (83%) pass rate (+6 tests)

DELIVERABLE:
- Interface fields accessible
- SAM conversion working for common cases

---

## Iteration 10: Inner Classes and Nested Types

**Status**: Ready for implementation  
**Expected Improvement**: +2-3 tests

### Background

Some MISSING_DEPENDENCY_CLASS errors reference nested classes (`JavaClass.Nested`, `Middle`). Inner class handling may need fixes.

### Prompt

---
TASK: Fix inner class and nested type resolution

CONTEXT:
**Expected Status After Iteration 9: ~115/138 (83%) tests passing**

IMPLEMENTATION STEPS:

1. **Identify failing tests**:
   - Tests with `MISSING_DEPENDENCY_CLASS: Cannot access class 'Nested'`
   - Tests with qualified names like `Outer.Inner`

2. **Verify current state**:
   - Check `JavaClassOverAst.findInnerClass()`
   - Check `JavaClassOverAst.innerClasses`
   - Check qualified name resolution in `classifierQualifiedName`

3. **Fix issues**:
   - Inner classes inherit outer class type parameters (non-static)
   - Static nested classes don't see outer type parameters
   - Qualified name resolution: `Outer.Inner` should find inner class

4. **Run tests and document**.

TARGET: 117/138 (85%) pass rate (+2 tests)

---

## Iteration 11: Atomics and Overload Resolution

**Status**: Ready for investigation  
**Expected Improvement**: +4-6 tests

### Background

Several tests fail with `NONE_APPLICABLE` (overload resolution) or `MISSING_DEPENDENCY_CLASS` for atomics:
- `testAddedOverloadWithAtomics`
- `testIntersectionKotlinJavaAtomics`
- `testKotlinToJavaHierarchy`
- `testUsingJavaAtomicWhenKotlinAtomicExpected`
- `testUsingKotlinAtomicWhenJavaAtomicExpected`

### Prompt

---
TASK: Investigate atomics and overload resolution failures

CONTEXT:
**Expected Status After Iteration 10: ~117/138 (85%) tests passing**

INVESTIGATION STEPS:

1. **Analyze atomic-related failures**:
   - What classes are missing? (`AtomicInteger`, etc.)
   - Are these from `java.util.concurrent.atomic` or `kotlin.concurrent`?

2. **Check overload resolution**:
   - `NONE_APPLICABLE` means no overload matches
   - Check if method signatures are correct
   - Check if type arguments are resolved properly

3. **Investigate Kotlin/Java atomics mapping**:
   - Kotlin has atomics that map to Java atomics
   - May need special handling in type conversion

4. **Implement fixes if straightforward**.

TARGET: 121/138 (88%) pass rate (+4 tests)

---

## Iteration 12: Error Handling and Diagnostics

**Status**: Lower priority  
**Expected Improvement**: Code quality, not test count

### Prompt

---
TASK: Improve error handling and diagnostic reporting

CONTEXT:
**Expected Status After Iteration 11: ~121/138 (88%) tests passing**

When resolution fails or Java code is malformed, we need clear error messages and graceful degradation.

IMPLEMENTATION STEPS:

1. **Identify error scenarios**:
   - Type not found
   - Malformed Java syntax
   - Circular dependencies

2. **Implement graceful degradation**:
   - Return error types instead of crashing
   - Provide useful error messages
   - Log resolution failures at debug level

3. **Test error handling** with intentional errors.

DELIVERABLE:
- Robust error handling
- Clear diagnostic messages

---

## Iteration 13: Performance and Caching

**Status**: Lower priority  
**Expected Improvement**: Performance, not test count

### Prompt

---
TASK: Optimize performance with proper caching strategies

CONTEXT:
**Expected Status After Iteration 12: ~121/138 (88%) tests passing**

IMPLEMENTATION STEPS:

1. **Review current caching**:
   - `JavaClassFinderOverAstImpl` caching
   - Lazy properties in AST classes

2. **Verify laziness**:
   - Supertypes, members, type parameters
   - Only parse files when requested

3. **Profile if needed**:
   - Measure test execution time
   - Identify bottlenecks before optimizing

DELIVERABLE:
- Optimized caching where needed
- Performance measurements

---

## Iteration 14: Final Validation and Documentation

**Status**: Final iteration

### Prompt

---
TASK: Validate implementation completeness and document findings

CONTEXT:
After previous iterations, most functionality should work.

VALIDATION STEPS:

1. **Run full test suite** and document pass/fail rates

2. **Analyze remaining failures**:
   - Group by failure type
   - Document as known limitations if not fixable

3. **Code quality check**:
   - Run `get_file_problems` on all modified files
   - Fix warnings related to changes

4. **Update documentation**:
   - `IMPLEMENTATION_PLAN.md` with final status
   - Known limitations
   - Recommendations for future work

DELIVERABLE:
- Test results summary
- Updated documentation
- Known limitations document

---

## Document Change Log

- 2026-03-04: **Major restructuring after Iteration 7c** (see `implDocs/PLANNING_CHANGELOG.md`):
  - Deleted old Iterations 8, 10 (Wildcards, Lower Bounds) - completed in 7c
  - Merged old Iterations 11+13 into new Iteration 8 (Annotations)
  - Added new Iteration 9 (SAM Conversion & Interface Fields)
  - Renumbered remaining iterations to 10-14
  - Current status: 101/138 (73.2%) tests passing
- 2026-03-04: Added Iteration 7c (Type Parameter Scope Resolution)
- 2026-03-04: Added Iteration 7b (ERROR_ELEMENT Import Handling)
- 2026-03-04: Added Iteration 7a (Array Types and Vararg Handling)
- 2026-03-04: Renumbered Iterations 7-15 to 8-16 to accommodate new iteration
- 2026-02-23: Updated Iterations 2-4 to reflect FIR-based resolution approach
- 2026-02-23: Split from ITERATIVE_FIXING_PLAN.md into separate iteration prompts
- 2026-02-10: Original content created in ITERATIVE_FIXING_PLAN.md
