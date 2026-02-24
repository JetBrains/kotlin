# Java-Direct: Fixing Iterations

## Document Purpose

This document contains structured iteration prompts for fixing issues in the `java-direct` module. Each iteration should be executed sequentially after confirming the previous iteration is complete.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Usage**: Execute one iteration at a time, following the 4-phase template  
**Status**: Ready for execution  
**Last Updated**: 2026-02-23

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

## Iteration 1: Initial Root Cause Analysis

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules, file locations, and tools.

### Prompt

---
TASK: Analyze failing tests in java-direct module to identify root causes

CONTEXT:
You are working on the Kotlin compiler's java-direct module. The module replaces 
IntelliJ platform-based Java parsing with a custom implementation. Tests are now 
running through the new implementation, but most are failing.

ANALYSIS STEPS:

1. SELECT TEST SAMPLE:
   - Look at generated tests in `compiler/java-direct/build/tests-gen/.../JavaUsingAstLegacyBoxTestGenerated.java`
   - Start with the first 3-5 simple-looking test methods
   - For each test, identify the test data location (shown in @TestMetadata annotation)

2. EXAMINE TEST DATA:
   - For each selected test, read the test data file from `compiler/testData/codegen/box/javaInterop/`
   - Identify what Java classes are defined (look for `// FILE: ClassName.java` markers)
   - Identify what Kotlin code is doing (usually has `fun box(): String` that returns "OK")
   - Understand the test scenario: what Java features are being tested?

3. RUN TESTS (if possible):
   - Try running one test via IDE or command line
   - Capture error output, stack traces, FIR diagnostics
   - Note: If running tests is difficult, proceed with code analysis

4. CODE TRACE:
   - Trace how `JavaClassFinderOverAstImpl` is invoked
   - Check if files are being indexed correctly (use logging/debugging insights)
   - Check if `findClass()` is returning `JavaClass` instances
   - Check what happens when FIR tries to use the returned `JavaClass`

5. IDENTIFY PATTERNS:
   - Are failures happening at parsing, indexing, class finding, or type resolution?
   - Are certain Java features causing more failures (generics, inheritance, annotations)?
   - Is there a common error message or exception?

6. ROOT CAUSE HYPOTHESIS:
   - Based on the analysis, propose 1-3 most likely root causes
   - For each root cause, explain the evidence supporting it
   - Rank them by likelihood and impact

DELIVERABLE:
Write a structured analysis document covering:
- Selected test cases and their scenarios
- Observed failure modes
- Code trace findings
- Root cause hypotheses (ranked)
- Recommended first fix to attempt

CONSTRAINTS:
- Focus on understanding, not fixing (yet)
- Be thorough but concise
- Use code references (file:line) for all findings
- Ask questions if test execution is needed but unclear how

CONFIRMATION REQUIRED: Present your analysis and wait for approval before proceeding.

---

## Iteration 2: Type Resolution - classifierQualifiedName

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules, `IMPLEMENTATION_PLAN.md` section 3.2.2, and `FIRSESSION_RESOLUTION_ANALYSIS.md`.

### Prompt

---
TASK: Fix JavaClassifierType to provide correct classifierQualifiedName for FIR resolution

CONTEXT:
Type resolution happens in the FIR layer, NOT in Java Model. Java Model's job is to:
1. Resolve LOCAL classes (same file) via LocalJavaScope → return in `classifier`
2. Provide correct type names via `classifierQualifiedName` → FIR resolves external types

See FIRSESSION_RESOLUTION_ANALYSIS.md for detailed rationale.

CRITICAL UNDERSTANDING:
FIR's JavaTypeConversion.kt (line 191-247) handles the case when `classifier == null`:
```kotlin
when (val classifier = classifier) {
    is JavaClass -> // Use classifier.classId
    is JavaTypeParameter -> // Use type parameter stack
    null -> {
        // Parse classifierQualifiedName as FqName
        val classId = ClassId.topLevel(FqName(this.classifierQualifiedName))
        classId.constructClassLikeType(...)
    }
}
```

IMPLEMENTATION STEPS:

1. REVIEW CURRENT STATE:
   - Read current `JavaClassifierTypeOverAst` implementation
   - Understand that `classifier` returns local classes only (via LocalJavaScope)
   - Understand that `classifierQualifiedName` must return correct FQN

2. CREATE TEST CASES:
   ```kotlin
   @Test
   fun testClassifierQualifiedName() {
       // Simple name (not qualified)
       val source1 = "class Derived extends Base {}"
       // classifierQualifiedName should be "Base"
       
       // Qualified name
       val source2 = "class MyClass extends java.util.ArrayList {}"
       // classifierQualifiedName should be "java.util.ArrayList"
       
       // Nested class reference
       val source3 = "class MyClass extends Outer.Inner {}"
       // classifierQualifiedName should be "Outer.Inner"
   }
   ```

3. FIX JavaClassifierTypeOverAst.classifierQualifiedName:
   - Current implementation just returns `node.text`
   - This is correct! No changes needed for simple/qualified names
   - Just verify it handles nested classes correctly (e.g., "Outer.Inner")

4. VERIFY JavaClassifierTypeOverAst.classifier:
   - Should return `localScope?.findClass(Name.identifier(simpleName))`
   - Returns null for external types (correct!)
   - Only resolves local classes

5. ADD isRaw DETECTION:
   - Check if type has type arguments in source
   - If declaration has type parameters but usage has none → raw type
   - Example: `List` (raw) vs `List<String>` (not raw)

6. TEST WITH BOX TESTS:
   - Run box tests with local inheritance (should pass)
   - Run box tests with java.lang types (should improve - FIR will resolve)
   - Document which tests now pass

DELIVERABLE:
- Fixed/verified JavaClassifierTypeOverAst.classifierQualifiedName
- Unit tests for qualified name extraction
- Report of box test improvements

CONSTRAINTS:
- DO NOT attempt to resolve external types in Java Model
- DO NOT try to access FirSession
- DO trust FIR to handle resolution for `classifier == null`

CONFIRMATION REQUIRED: Show your understanding of the approach before coding.

---

## Iteration 3: Import Handling and Name Qualification

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules, `IMPLEMENTATION_PLAN.md` section 3.4, and `FIRSESSION_RESOLUTION_ANALYSIS.md`.

### Prompt

---
TASK: Implement import statement tracking to improve classifierQualifiedName accuracy

CONTEXT:
After Iteration 2, we can extract type names but they're not qualified. When source says:
```
import java.util.ArrayList;
class MyClass extends ArrayList {}
```
We need `classifierQualifiedName` to return "java.util.ArrayList", not "ArrayList".

FIR will then resolve "java.util.ArrayList" using session.symbolProvider.

CRITICAL: We are NOT implementing resolution here, just name qualification!

IMPLEMENTATION STEPS:

1. CREATE TEST CASES:
   ```kotlin
   @Test
   fun testImportExtraction() {
       val source = """
           package test;
           import java.util.ArrayList;
           import java.util.concurrent.atomic.*;
           
           class MyClass extends ArrayList {
               AtomicInteger counter;
           }
       """.trimIndent()
       
       // Verify imports extracted correctly
       // Verify ArrayList qualified to "java.util.ArrayList"
       // Verify AtomicInteger kept as "AtomicInteger" (star import)
   }
   ```

2. IMPLEMENT IMPORT EXTRACTION:
   - Create `JavaImports` data class:
     ```kotlin
     data class JavaImports(
         val simpleImports: Map<String, FqName>,  // "ArrayList" -> "java.util.ArrayList"
         val starImports: List<FqName>             // ["java.util.concurrent.atomic"]
     )
     ```
   - Parse import statements from AST
   - Handle both single-type and star imports
   - Skip static imports for now (not critical)

3. ENHANCE JavaClassifierTypeOverAst:
   - Add `imports: JavaImports?` parameter
   - Update `classifierQualifiedName` to check simple imports:
     ```kotlin
     override val classifierQualifiedName: String by lazy {
         val typeName = node.text
         
         // Already qualified?
         if (typeName.contains('.')) return typeName
         
         // Check simple imports
         imports?.simpleImports?.get(typeName)?.asString()
             ?: typeName  // Keep simple name, FIR will check star imports
     }
     ```

4. WIRE IMPORTS THROUGH PARSING:
   - Extract imports when parsing file
   - Pass to LocalJavaScope or store separately
   - Pass to JavaClassifierTypeOverAst during construction

5. TEST WITH BOX TESTS:
   - Run box tests with imports
   - Verify types are qualified correctly
   - FIR should now resolve imported types

6. DOCUMENT STAR IMPORTS:
   - Store star imports in JavaImports
   - DO NOT attempt to resolve them in Java Model
   - FIR will handle: check current package, then star imports, then java.lang

DELIVERABLE:
- JavaImports data class (with simpleImports and starImports)
- Import extraction logic
- Enhanced JavaClassifierTypeOverAst
- Unit tests
- Box test improvement report

CONSTRAINTS:
- DO use correct FIR terminology: `simpleImports`, `starImports` (not singleType/onDemand)
- DO NOT attempt to resolve star imports in Java Model
- DO trust FIR to handle star imports and java.lang

CONFIRMATION REQUIRED: Show import extraction logic before implementing.

---

## Iteration 4: Validation and FIR Integration Check

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `FIRSESSION_RESOLUTION_ANALYSIS.md`.

### Prompt

---
TASK: Verify that FIR successfully resolves types using our classifierQualifiedName

CONTEXT:
After Iterations 2-3:
- Local classes are resolved via `classifier` (LocalJavaScope)
- External classes return `classifier = null` with correct `classifierQualifiedName`
- Simple imports are handled

Now verify FIR actually resolves external types correctly.

UNDERSTANDING THE FLOW:
1. Java Model provides JavaClassifierType with `classifierQualifiedName`
2. FIR wraps it in FirJavaTypeRef
3. FIR calls `JavaTypeConversion.toConeKotlinTypeForFlexibleBound()`
4. This uses `session.symbolProvider` to resolve the class
5. Creates proper FirTypeRef with resolved type

IMPLEMENTATION STEPS:

1. ADD DIAGNOSTIC LOGGING (temporary):
   - Add logging in JavaClassifierTypeOverAst to see what names we provide
   - This helps debug if FIR resolution fails

2. RUN BOX TESTS WITH STANDARD LIBRARY:
   - Find tests that use java.lang.Object, java.lang.String
   - Find tests that use java.util collections
   - Run and check if they pass or fail differently

3. ANALYZE FAILURES:
   - If tests still fail with "UNRESOLVED_REFERENCE", check:
     * Is `classifierQualifiedName` correct?
     * Is the type actually resolvable by FIR?
     * Are there other issues (constructors, methods)?
   - Group failures by type

4. VERIFY PACKAGE RESOLUTION:
   - Test classes in same package referencing each other
   - FIR should resolve using current package + class name
   - Example: package `test`, classes `Base` and `Derived extends Base`

5. CHECK STAR IMPORTS:
   - Test files with `import java.util.*` and using `ArrayList`
   - `classifierQualifiedName` should be "ArrayList" (simple name)
   - FIR should check star imports and resolve to java.util.ArrayList

6. DOCUMENT WHAT WORKS:
   - List which resolution scenarios work
   - List which still fail (and why)
   - Estimate box test pass rate

DELIVERABLE:
- Box test results analysis
- List of working resolution scenarios
- List of remaining issues (for future iterations)
- Updated success metrics

CHALLENGES:
- Some failures may not be resolution issues but other problems
  (constructors, generics, annotations, etc.)
- Need to distinguish resolution failures from other failures

CONFIRMATION REQUIRED: Show analysis approach and test selection.

---

## Iteration 5: Package and Multi-File Resolution

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.1.

### Prompt

---
TASK: Handle Java files in different packages referencing each other

CONTEXT:
Tests often have multiple Java files in different packages. These need to find 
each other via qualified names or imports.

IMPLEMENTATION STEPS:

1. CREATE TEST CASE:
   ```kotlin
   @Test
   fun testMultiPackage() {
       // Simulate two files:
       // com/example/Base.java: public class Base {}
       // test/Derived.java: import com.example.Base; class Derived extends Base {}
       
       // This requires proper indexing and package-based lookup
   }
   ```

2. VERIFY INDEXING:
   - Check JavaClassFinderOverAstImpl.buildIndex()
   - Ensure all packages are indexed
   - Verify package names extracted correctly

3. VERIFY PACKAGE LOOKUP:
   - Check JavaPackageOverAst implementation
   - Ensure findClass works across packages
   - Handle package hierarchies correctly

4. TEST WITH BOX TESTS:
   - Find box tests with multiple packages
   - Run and verify they pass
   - Fix any remaining issues

DELIVERABLE:
- Fixed package handling (if needed)
- Multi-package test cases
- Box test improvement report

CONFIRMATION REQUIRED: Check if package handling already works, or if fixes needed.

---

## Iteration 6: Generic Types and Type Parameters

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.3.

### Prompt

---
TASK: Implement proper handling of Java generic types

CONTEXT:
Many box tests use generics: `List<String>`, `Map<K, V>`, type parameters, wildcards.
These need proper representation and resolution.

IMPLEMENTATION STEPS:

1. REVIEW EXISTING CODE:
   - Check JavaTypeParameterOverAst
   - Check JavaClassifierTypeOverAst
   - Understand current limitations

2. CREATE TEST CASES:
   ```kotlin
   @Test
   fun testTypeParameters() {
       val source = """
           class Generic<T> {
               public T value;
           }
       """.trimIndent()
       // Verify type parameter extraction
   }
   
   @Test
   fun testParameterizedTypes() {
       val source = """
           import java.util.List;
           class MyClass {
               public List<String> items;
           }
       """.trimIndent()
       // Verify type arguments
   }
   ```

3. IMPLEMENT TYPE PARAMETER RESOLUTION:
   - Type parameters should be checked first in resolution
   - Use MutableJavaTypeParameterStack (see IMPLEMENTATION_PLAN.md section 3.3)
   - Handle bounds: `<T extends Comparable<T>>`

4. IMPLEMENT PARAMETERIZED TYPES:
   - Extract type arguments from AST
   - Recursively resolve each argument
   - Handle wildcards: `?`, `? extends T`, `? super T`

5. TEST AND VERIFY:
   - Unit tests pass
   - Box tests with generics start passing

DELIVERABLE:
- Enhanced generic type handling
- Type parameter resolution
- Test cases
- Box test improvement report

CONFIRMATION REQUIRED: Review current generic handling before making changes.

---

## Iteration 7: Annotation Support

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.5.

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

## Iteration 8: Error Handling and Diagnostics

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules.

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

## Iteration 9: Performance and Caching

**Reference**: See `AGENT_INSTRUCTIONS.md` for common pitfalls about laziness.

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

## Iteration 10: Final Validation and Documentation

**Reference**: See `AGENT_INSTRUCTIONS.md` for success metrics.

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

- 2026-02-23: Updated Iterations 2-4 to reflect FIR-based resolution approach (see FIRSESSION_RESOLUTION_ANALYSIS.md)
- 2026-02-23: Iteration 2 now focuses on classifierQualifiedName correctness, not FirSession access
- 2026-02-23: Iteration 3 now focuses on import tracking for name qualification
- 2026-02-23: Iteration 4 now validates FIR resolution integration, not FirSession plumbing
- 2026-02-23: Split from ITERATIVE_FIXING_PLAN.md into separate iteration prompts
- 2026-02-10: Original content created in ITERATIVE_FIXING_PLAN.md
