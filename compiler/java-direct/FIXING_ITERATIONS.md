# Java-Direct: Fixing Iterations

## Document Purpose

This document contains structured iteration prompts for fixing issues in the `java-direct` module. Each iteration should be executed sequentially after confirming the previous iteration is complete.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Usage**: Execute one iteration at a time, following the 4-phase template  
**Status**: Iteration 7 analysis complete, ready for implementation  
**Last Updated**: 2026-03-04

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

## Iteration 8: Wildcards and Complex Generics

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md` section 3.3.
**Note**: Renumbered from original Iteration 7 after adding new Iteration 7 (Array Types).

### Prompt

---
TASK: Implement wildcard type arguments and simple upper bounds

CONTEXT:
**Expected Status After Iteration 5: ~40-50/138 (29-36%) tests passing**

After simple type arguments work, many tests will need:
- Wildcards: `List<?>`, `List<? extends Number>`
- Upper bounds: `<T extends Comparable<T>>`

**Incremental Approach**: Handle wildcards and upper bounds, defer lower bounds (`? super`) to later.

IMPLEMENTATION STEPS:

### Phase 1: Wildcard Support

1. IDENTIFY WILDCARD IN AST:
   - Look for `QUESTION` or `WILDCARD` node in TYPE_ARGUMENT
   - Check for `EXTENDS_KEYWORD` following wildcard
   - Example AST: `List<? extends Number>`

2. CREATE TEST CASES:
   ```kotlin
   @Test
   fun testWildcards() {
       val source = """
           import java.util.List;
           class MyClass {
               public List<?> wildcardList;
               public List<? extends Number> boundedList;
           }
       """.trimIndent()
       
       // Verify wildcard detected
       // Verify bound extracted
   }
   ```

3. IMPLEMENT WILDCARD PARSING:
   - Update type argument parsing to detect wildcard marker
   - Extract bound type if `extends` present
   - Create appropriate `JavaWildcardType` representation
   - Reference javac-wrapper/PSI for correct structure

### Phase 2: Type Parameter Bounds

4. PARSE TYPE PARAMETER BOUNDS:
   - In `JavaTypeParameterOverAst`, parse `EXTENDS_BOUND_LIST`
   - Extract bound types (usually one, can be multiple for interfaces)
   - Store as `upperBounds` collection

5. TEST TYPE PARAMETERS:
   ```kotlin
   @Test
   fun testTypeParameterBounds() {
       val source = """
           class Generic<T extends Number> {
               public T value;
           }
       """.trimIndent()
       
       // Verify type parameter has bound
       val typeParam = javaClass.typeParameters[0]
       assertEquals(1, typeParam.upperBounds.size)
   }
   ```

### Phase 3: Validate and Test

6. RUN TESTS:
   - Unit tests for wildcards
   - Unit tests for bounded type parameters
   - Box tests (expect +5-15 more tests)

7. TARGET: 50-65/138 (36-47%) pass rate

DELIVERABLE:
- Wildcard parsing
- Type parameter bounds
- Box test improvement report

CONFIRMATION REQUIRED: Complete Iteration 5 first, then assess if this is the right next step.

---

## Iteration 9: Kotlin Class Resolution from Java

**Reference**: See `AGENT_INSTRUCTIONS.md` and `implDocs/ITERATION_7_PROBLEM_ANALYSIS.md`.
**Note**: Renumbered from original Iteration 8; content updated based on iteration 7 analysis.

### Prompt

---
TASK: Investigate and fix Kotlin class resolution from Java code

CONTEXT:
**Expected Status After Iteration 7: ~95-100/138 (69-72%) tests passing**

15 tests fail with MISSING_DEPENDENCY_CLASS when Java code imports Kotlin classes like:
```java
import kotlin.Function;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.FunctionN;
```

INVESTIGATION STEPS:

1. UNDERSTAND THE PROBLEM:
   - Java sources import Kotlin classes from kotlin-stdlib
   - `CombinedJavaClassFinder` falls back to binary finder
   - Binary finder may not be configured to find Kotlin classes

2. CHECK BINARY FINDER CONFIGURATION:
   - Examine how `defaultFinderProvider` is set up in `JavaDirectComponentRegistrar`
   - Verify kotlin-stdlib is on the classpath for the binary finder
   - Check if Kotlin class metadata is accessible

3. POTENTIAL FIXES:
   - Ensure binary finder includes kotlin-stdlib JAR
   - May need to configure additional class paths
   - Consider if Kotlin classes need special handling

4. TEST WITH SIMPLE CASE:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testLambdaInstanceOf" -q
   ```

5. TARGET: 105-115/138 (76-83%) pass rate if fixed

DELIVERABLE:
- Analysis of Kotlin class resolution issue
- Fix if straightforward, or detailed investigation report
- Documentation of findings

CONFIRMATION REQUIRED: Assess complexity after initial investigation.

---

## Iteration 10: Lower Bounds and Complex Generics

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md`.
**Note**: Renumbered from original Iteration 9.

### Prompt

---
TASK: Implement lower bounds (`super`) and complex generic patterns

CONTEXT:
**Expected Status After Iteration 7: ~45-60/138 (33-43%) tests passing**

After implementing wildcards, upper bounds, and arrays, tackle remaining generic complexity.

IMPLEMENTATION STEPS:

1. IMPLEMENT LOWER BOUNDS:
   - Parse `? super T` syntax
   - Create appropriate type variance in FIR
   - Test with consumer patterns like `List<? super Integer>`

2. HANDLE COMPLEX NESTED GENERICS:
   - `Map<String, List<Integer>>`
   - `Function<List<String>, Map<Integer, String>>`
   - Recursive generic bounds

3. TEST VARIANCE COMBINATIONS:
   - `List<? extends List<? super Number>>`
   - Mixed variance in multi-parameter types

4. CREATE COMPREHENSIVE TESTS:
   ```kotlin
   @Test
   fun testLowerBounds() {
       val source = """
           import java.util.*;
           class Consumer {
               void accept(List<? super Integer> list) {}
           }
       """.trimIndent()
       // Verify lower bound parsing
   }
   ```

5. RUN BOX TESTS:
   - Expect +5-10 additional tests

6. TARGET: 50-65/138 (36-47%) pass rate

SCOPE LIMITATIONS:
- Focus on type representation, not full variance checking
- Complex recursive bounds may have limitations

DELIVERABLE:
- Lower bound support
- Complex generics handling
- Test improvements

CONFIRMATION REQUIRED: Assess after Iteration 7 completion.

---

## Iteration 11: Annotations and Nullability

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md` section 3.5.
**Note**: Renumbered from original Iteration 10.

### Prompt

---
TASK: Improve annotation support, especially nullability annotations

CONTEXT:
**Expected Status After Iteration 8: ~50-65/138 (36-47%) tests passing**

Nullability annotations (`@Nullable`, `@NotNull`) affect type checking. Better annotation parsing may unlock more tests.

IMPLEMENTATION STEPS:

1. REVIEW CURRENT ANNOTATIONS:
   - Check what `JavaAnnotationOwner.annotations` returns
   - Verify annotation arguments parsed

2. FOCUS ON NULLABILITY:
   - Parse `@Nullable`, `@NotNull` annotations
   - Attach to appropriate declarations
   - FIR uses these for null-safety checks

3. CREATE TESTS:
   ```kotlin
   @Test
   fun testNullabilityAnnotations() {
       val source = """
           import org.jetbrains.annotations.NotNull;
           class MyClass {
               public @NotNull String name;
           }
       """.trimIndent()
       
       // Verify annotation present on field
   }
   ```

4. RUN BOX TESTS:
   - Expect +5-10 tests with nullability checks

5. TARGET: 60-75/138 (43-54%) pass rate

DELIVERABLE:
- Enhanced annotation support
- Nullability annotations working
- Box test improvements

CONFIRMATION REQUIRED: Assess after Iteration 6 if this is next priority.

---

## Iteration 12: Inner Classes and Nested Types

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md`.
**Note**: Renumbered from original Iteration 11.

### Prompt

---
TASK: Implement proper inner class and nested type support

CONTEXT:
Some tests use inner classes, nested classes, or reference types as `Outer.Inner`.

IMPLEMENTATION STEPS:

1. VERIFY CURRENT STATE:
   - `findInnerClass()` exists
   - Check if nested class references work

2. TEST PATTERNS:
   ```java
   class Outer {
       class Inner { }
       static class Nested { }
   }
   
   class Usage extends Outer.Inner { }
   ```

3. FIX ISSUES:
   - Qualified name resolution for nested classes
   - Proper inner vs static nested distinction
   - Outer class references

4. RUN BOX TESTS:
   - Expect +5-10 tests

5. TARGET: 70-85/138 (51-62%) pass rate

DELIVERABLE:
- Inner class support
- Test improvements

CONFIRMATION REQUIRED: Assess priority after earlier iterations.

---

## Iteration 13: Annotation Support (Extended)

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.5.
**Note**: Renumbered from original Iteration 12.

### Prompt

---
TASK: Implement annotation extraction and argument handling

CONTEXT:
Some tests use annotations, especially nullability annotations (@NotNull, @Nullable).
These affect type inference and null safety.

IMPLEMENTATION STEPS:

1. REVIEW EXISTING CODE:
   - Check JavaAnnotationOverAst
   - See what's already implemented

2. CREATE TEST CASES:
   ```kotlin
   @Test
   fun testAnnotations() {
       val source = """
           import org.jetbrains.annotations.NotNull;
           class MyClass {
               @NotNull
               public String getValue() { return ""; }
           }
       """.trimIndent()
       // Verify annotation extraction
   }
   ```

3. IMPLEMENT BASIC ANNOTATIONS:
   - Extract annotations from AST
   - Resolve annotation class IDs
   - Handle common annotations (@NotNull, @Nullable, @Override)

4. DEFER COMPLEX FEATURES:
   - Annotation arguments with constants: defer per IMPLEMENTATION_PLAN.md section 4
   - Implement only literals for now
   - Mark complex cases as TODO

5. TEST AND VERIFY:
   - Annotation tests pass
   - Box tests with annotations improve

DELIVERABLE:
- Enhanced annotation handling
- Test cases
- Documentation of limitations
- Box test improvement report

CONFIRMATION REQUIRED: Verify current state of annotation support first.

---

## Iteration 14: Error Handling and Diagnostics

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules.
**Note**: Renumbered from original Iteration 13.

### Prompt

---
TASK: Improve error handling and diagnostic reporting

CONTEXT:
When resolution fails or Java code is malformed, we need clear error messages
and graceful degradation.

IMPLEMENTATION STEPS:

1. IDENTIFY ERROR SCENARIOS:
   - Type not found
   - Package not found
   - Malformed Java syntax
   - Circular dependencies

2. IMPLEMENT ERROR TYPES:
   - Create JavaErrorType for unresolved types
   - Ensure FIR can handle error types
   - Provide useful error messages

3. ADD LOGGING:
   - Log resolution attempts (at debug level)
   - Log failures with context
   - Help debugging future issues

4. HANDLE EDGE CASES:
   - Missing files
   - Parse errors
   - Invalid type references

5. TEST ERROR HANDLING:
   - Create tests with intentional errors
   - Verify graceful failure
   - Check error messages are useful

DELIVERABLE:
- Robust error handling
- Clear diagnostic messages
- Error test cases
- Documentation

CONFIRMATION REQUIRED: Discuss error handling strategy.

---

## Iteration 15: Performance and Caching

**Reference**: See `AGENT_INSTRUCTIONS.md` for common pitfalls about laziness.
**Note**: Renumbered from original Iteration 14.

### Prompt

TASK: Optimize performance with proper caching strategies

CONTEXT:
Parsing and resolution should be lazy and cached to avoid redundant work.

IMPLEMENTATION STEPS:

1. REVIEW CURRENT CACHING:
   - Check JavaClassFinderOverAstImpl caching
   - Check JavaClassOverAst lazy properties
   - Identify what's cached vs recomputed

2. ADD MISSING CACHES:
   - Cache parsed AST if needed
   - Cache resolved types
   - Cache import information

3. VERIFY LAZINESS:
   - Ensure supertypes are lazy
   - Ensure members are lazy
   - Only parse files when actually requested

4. MEASURE PERFORMANCE:
   - Run box tests and measure time
   - Compare with PSI-based implementation (if possible)
   - Identify bottlenecks

5. OPTIMIZE HOT PATHS:
   - Optimize indexing if slow
   - Optimize type resolution if slow
   - Don't over-optimize prematurely

DELIVERABLE:
- Optimized caching
- Performance measurements
- Documentation of caching strategy

CONFIRMATION REQUIRED: Profile first to find actual bottlenecks.

---

## Iteration 16: Final Validation and Documentation

**Reference**: See `AGENT_INSTRUCTIONS.md` for success metrics.
**Note**: Renumbered from original Iteration 15.

### Prompt

TASK: Validate implementation completeness and document findings

CONTEXT:
After previous iterations, most functionality should work. Final validation ensures
quality and completeness.

VALIDATION STEPS:

1. RUN FULL TEST SUITE:
   - Run all generated box tests
   - Document pass/fail rates
   - Categorize remaining failures

2. ANALYZE REMAINING FAILURES:
   - Group by failure type
   - Determine if fixable in current scope
   - Document as known limitations if not

3. CODE QUALITY CHECK:
   - Run mcp__jetbrains__get_file_problems on all modified files
   - Fix warnings related to changes
   - Ensure code follows Kotlin conventions

4. UPDATE DOCUMENTATION:
   - Update IMPLEMENTATION_PLAN.md with current status
   - Document known limitations
   - List deferred features (annotation arguments, etc.)

5. CREATE SUMMARY REPORT:
   - What was implemented
   - What works now vs before
   - What still needs work
   - Recommendations for next steps

DELIVERABLE:
- Test results summary
- Updated documentation
- Known limitations document
- Recommendations for future work

CONFIRMATION REQUIRED: N/A (final iteration)

---

## Document Change Log

- 2026-03-04: Added Iteration 7 (Array Types and Type Parameter Bounds) based on systematic failure analysis
- 2026-03-04: Renumbered Iterations 7-15 to 8-16 to accommodate new iteration
- 2026-03-04: Updated Iteration 9 (formerly 8) with Kotlin class resolution investigation
- 2026-02-23: Updated Iterations 2-4 to reflect FIR-based resolution approach (see FIRSESSION_RESOLUTION_ANALYSIS.md)
- 2026-02-23: Iteration 2 now focuses on classifierQualifiedName correctness, not FirSession access
- 2026-02-23: Iteration 3 now focuses on import tracking for name qualification
- 2026-02-23: Iteration 4 now validates FIR resolution integration, not FirSession plumbing
- 2026-02-23: Split from ITERATIVE_FIXING_PLAN.md into separate iteration prompts
- 2026-02-10: Original content created in ITERATIVE_FIXING_PLAN.md
