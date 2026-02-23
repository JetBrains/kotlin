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
3. **Between iterations**: Get user confirmation to proceed to next iteration

---

## Iteration 1: Initial Root Cause Analysis

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules, file locations, and tools.

### Prompt

```
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
```

---

## Iteration 2: Type Resolution Implementation

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.2.

### Prompt

```
TASK: Implement basic type resolution for Java sources

CONTEXT:
Based on Iteration 1 analysis, type resolution is likely a major issue. Java types 
referenced in source files (in supertypes, field types, method return types) are not 
being resolved correctly. This iteration implements the resolution mechanism.

BACKGROUND:
- See IMPLEMENTATION_PLAN.md section 3.2.2 "Lazy Type Resolution Architecture"
- Resolution needs two layers:
  1. Local scope: Classes defined in the same Java file
  2. FIR delegation: External classes via FirSession.symbolProvider

IMPLEMENTATION STEPS:

1. CREATE TEST CASE:
   - Create `compiler/java-direct/test/.../TypeResolutionTest.kt`
   - Start with simplest case:
     ```kotlin
     @Test
     fun testSimpleInheritance() {
         val source = """
             package test;
             class Base {}
             class Derived extends Base {}
         """.trimIndent()
         
         // Parse and verify Derived.supertypes contains Base
     }
     ```

2. IMPLEMENT LOCAL SCOPE:
   - Create `LocalJavaScope` class (see IMPLEMENTATION_PLAN.md section 3.2.3)
   - Extract class names while parsing
   - Build map: className -> JavaClass
   - Make scope available to JavaClassOverAst

3. ENHANCE JavaClassOverAst:
   - Add `localScope: LocalJavaScope` parameter
   - Modify `supertypes` getter to resolve references:
     ```kotlin
     override val supertypes: Collection<JavaClassifierType> by lazy {
         // Extract type names from AST
         // Try local scope first
         // Fall back to unresolved type (for now)
     }
     ```

4. TEST AND VERIFY:
   - Run TypeResolutionTest
   - Verify local inheritance works
   - Check that some box tests start passing

5. DOCUMENT LIMITATIONS:
   - List what still doesn't work (FIR delegation, imports, etc.)
   - These will be fixed in next iterations

DELIVERABLE:
- LocalJavaScope class
- Enhanced JavaClassOverAst with lazy resolution
- TypeResolutionTest with passing tests
- Report of box test improvements

CONFIRMATION REQUIRED: Show your implementation plan and test cases before coding.
```

---

## Iteration 3: FIR Symbol Provider Integration

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.2.

### Prompt

```
TASK: Integrate FIR symbol provider for resolving external Java types

CONTEXT:
After Iteration 2, local type resolution works. Now we need to resolve types from:
- Java standard library (java.lang.*, java.util.*, etc.)
- Other packages in the project
- Dependencies (external jars)

These are resolved via FirSession.symbolProvider, not by our parser.

IMPLEMENTATION STEPS:

1. CREATE TEST CASE:
   - Add test in TypeResolutionTest:
     ```kotlin
     @Test
     fun testStandardLibraryTypes() {
         val source = """
             package test;
             import java.util.List;
             class MyClass {
                 public List<String> items;
             }
         """.trimIndent()
         
         // Parse and verify field type resolves to java.util.List
     }
     ```

2. CREATE JavaTypeResolver:
   - See IMPLEMENTATION_PLAN.md section 3.2.2 "Phase 2: Resolution via FIR"
   - Implement resolution hierarchy:
     1. Type parameters of current class
     2. Local scope (same file)
     3. Current package
     4. java.lang package
     5. Imports (to be implemented later)

3. INTEGRATE WITH FIR:
   - Get FirSession from context (needs plumbing)
   - Query `firSession.symbolProvider.getClassLikeSymbolByClassId(classId)`
   - Handle null results gracefully (return error type)

4. CREATE FIR SYMBOL WRAPPER:
   - Implement `JavaClassFromFirSymbol` (see IMPLEMENTATION_PLAN.md section 3.2.4)
   - This wraps FirRegularClassSymbol as JavaClass
   - Implement minimum needed: name, fqName, basic members

5. WIRE EVERYTHING:
   - Pass FirSession to JavaClassFinderOverAstImpl (via factory)
   - Pass to JavaClassOverAst during construction
   - Use in type resolution

6. TEST AND VERIFY:
   - Unit tests pass
   - Box tests with java.lang types start passing
   - Check for regressions

CHALLENGES:
- Getting FirSession to JavaClassFinderOverAstImpl may require changes to factory
- Circular dependency risk: FIR needs Java model, Java model needs FIR
- Solution: Lazy resolution breaks the cycle

DELIVERABLE:
- JavaTypeResolver class
- JavaClassFromFirSymbol wrapper
- Modified factory to pass FirSession
- Updated tests
- Box test improvement report

CONFIRMATION REQUIRED: Discuss FirSession plumbing approach before implementing.
```

---

## Iteration 4: Import Handling

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.4.

### Prompt

```
TASK: Implement import statement tracking and resolution

CONTEXT:
Java classes use imports to reference types from other packages. Currently these 
are not tracked, so resolution fails for imported types.

IMPLEMENTATION STEPS:

1. CREATE TEST CASE:
   ```kotlin
   @Test
   fun testImports() {
       val source = """
           package test;
           import java.util.ArrayList;
           import java.util.concurrent.atomic.*;
           
           class MyClass {
               public ArrayList<String> list;
               public AtomicInteger counter;
           }
       """.trimIndent()
       
       // Verify both single-type and star imports work
   }
   ```

2. IMPLEMENT IMPORT EXTRACTION:
   - See IMPLEMENTATION_PLAN.md section 3.4 "Import Handling"
   - Use correct FIR terminology (MANDATORY):
     * `simpleImports` (not singleTypeImports)
     * `starImports` (not onDemandImports)
   - Extract during initial parsing
   - Store in `JavaImports` data class

3. INTEGRATE WITH RESOLUTION:
   - Pass JavaImports to JavaTypeResolver
   - Check simpleImports before current package
   - Check starImports after current package
   - Maintain correct resolution order per Java spec

4. HANDLE EDGE CASES:
   - Static imports (skip for now - not critical)
   - Import of inner classes (e.g., `import outer.Outer.Inner`)
   - Conflicts between imports

5. TEST AND VERIFY:
   - Unit tests with various import patterns
   - Box tests with imports should start passing
   - Verify no regressions

DELIVERABLE:
- Import extraction logic
- JavaImports data class (with simpleImports and starImports)
- Updated JavaTypeResolver
- Test cases
- Box test improvement report

CONFIRMATION REQUIRED: Present import extraction strategy before coding.
```

---

## Iteration 5: Package and Multi-File Resolution

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.1.

### Prompt

```
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
```

---

## Iteration 6: Generic Types and Type Parameters

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.3.

### Prompt

```
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
```

---

## Iteration 7: Annotation Support

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `IMPLEMENTATION_PLAN.md` section 3.5.

### Prompt

```
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
```

---

## Iteration 8: Error Handling and Diagnostics

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules.

### Prompt

```
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
```

---

## Iteration 9: Performance and Caching

**Reference**: See `AGENT_INSTRUCTIONS.md` for common pitfalls about laziness.

### Prompt

```
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
```

---

## Iteration 10: Final Validation and Documentation

**Reference**: See `AGENT_INSTRUCTIONS.md` for success metrics.

### Prompt

```
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
```

---

## Document Change Log

- 2026-02-23: Split from ITERATIVE_FIXING_PLAN.md into separate iteration prompts
- 2026-02-10: Original content created in ITERATIVE_FIXING_PLAN.md
