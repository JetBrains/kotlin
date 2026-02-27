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
TASK: Find and deeply analyze ONE representative failing test

CONTEXT:
You are working on the Kotlin compiler's java-direct module. Many tests fail.
Your job: Pick ONE test, understand it completely, fix it.

CRITICAL MINDSET SHIFT:
- ❌ Don't analyze "patterns across all failures"
- ❌ Don't categorize 100+ test failures
- ✅ Pick ONE concrete failing test
- ✅ Make that ONE test pass
- ✅ Measure how many similar tests also pass

ANALYSIS STEPS:

### Step 1: Get Error Statistics (5 minutes max)

Run full test suite ONCE to see what errors are most common:

```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q 2>&1 | tee full_test_output.txt
```

Extract error frequency:
```bash
grep -E "(MISSING_DEPENDENCY|UNRESOLVED_REFERENCE|TYPE_MISMATCH|ABSTRACT_MEMBER)" full_test_output.txt | sort | uniq -c | sort -rn | head -10
```

**Pick the most frequent error type.** This is your target.

### Step 2: Find Simplest Test With That Error (10 minutes max)

Look through test output for the simplest test showing that error:
- Shortest test file (fewest lines)
- Fewest Java features (no generics, annotations, nested classes if possible)
- Clear, focused scenario

Example:
```bash
# If UNRESOLVED_REFERENCE '<init>' is most common (128 occurrences)
# Find which test has shortest Java code with <init> error
grep -B 5 "UNRESOLVED_REFERENCE: '<init>'" full_test_output.txt | grep "testData" | head -5
```

**Select ONE test.** Write it down. Example: `testAbstractMethodsOfAny`

### Step 3: Run ONLY That Test (repeat as needed)

```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testAbstractMethodsOfAny" -q
```

### Step 4: Deep Dive Into That ONE Test (no time limit)

1. **Read the test data file:**
   ```bash
   # Test metadata shows path like compiler/testData/codegen/box/javaInterop/abstractMethodsOfAny.kt
   cat compiler/testData/codegen/box/javaInterop/abstractMethodsOfAny.kt
   ```

2. **Understand what it tests:**
   - What Java class(es) are defined? (look for `// FILE: ClassName.java`)
   - What Kotlin code uses them?
   - What should happen? (usually `fun box()` returns "OK" if test passes)

3. **Examine the exact error:**
   - What line fails?
   - What is the exact error message?
   - What values are involved?

4. **Trace execution for THIS test:**
   - Add logging/print statements if needed
   - Set breakpoints if using IDE
   - Follow the code path step by step
   - Find WHERE and WHY it fails

5. **Form concrete hypothesis:**
   - Not "type resolution doesn't work"
   - But "When parsing class Foo, hasDefaultConstructor() returns false but should return true because..."

### Step 5: Create Unit Test Reproducing the Issue

Extract the minimal case:

```kotlin
@Test
fun testReproduceAbstractMethodsOfAny() {
    val source = """
        // FILE: J.java
        // Paste minimal Java code from the box test
        public abstract class J {
            public abstract Object foo();
        }
    """.trimIndent()
    
    // Parse and verify what goes wrong
    val javaClass = parseJavaClass(source, "J")
    
    // The assertion that should pass but doesn't:
    assertTrue(javaClass.hasDefaultConstructor())
    // Or whatever specific thing is broken
}
```

Run ONLY this unit test until you understand the issue.

### Step 6: Fix and Verify

1. Implement the fix
2. Run your unit test → should pass
3. Run the ONE box test → should pass
4. Run ~10 similar box tests → count improvements
5. Run full suite → measure overall progress

DELIVERABLE:
Write a structured analysis focusing on THE ONE TEST:
- Test name: `testAbstractMethodsOfAny`
- What it tests: "Kotlin abstract class inherits from Java abstract class with Object methods"
- Exact error: "UNRESOLVED_REFERENCE: '<init>' at line X"
- Root cause: "hasDefaultConstructor() hardcoded to false, should check constructors.isEmpty()"
- Fix: "Change line 113 in JavaClassOverAst.kt"
- Verification: "testAbstractMethodsOfAny passes, 15 other <init> tests now pass, 12/138 → 27/138"

CONSTRAINTS:
- Spend 80% time on ONE test, 20% on measuring impact
- Do NOT try to fix all patterns at once
- Do NOT categorize all 138 test failures
- DO make ONE test pass first

CONFIRMATION REQUIRED: 
After Step 2, state: "I selected test X because Y. I will now focus exclusively on making this test pass."
Wait for approval before proceeding to implementation.

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

## Iteration 4: Star Import Resolution via Callback

**Reference**: See `AGENT_INSTRUCTIONS.md` for ground rules and `TYPE_RESOLUTION_DESIGN.md`.

### Prompt

---
TASK: Implement star import and java.lang resolution using the callback approach

CONTEXT:
After Iterations 2-3, we handle:
- ✅ Local classes (via `classifier`)
- ✅ Fully qualified names (`java.util.ArrayList`)
- ✅ Single-type imports (`import java.util.ArrayList;`)
- ❌ Star imports (`import java.util.*;` then `List`)
- ❌ java.lang automatic import (`Object` should resolve to `java.lang.Object`)

Current state: 11/138 (7%) box tests pass
Expected after this iteration: ~120-130/138 (87-94%) pass

SOLUTION: Resolve callback approach per TYPE_RESOLUTION_DESIGN.md
- Java Model implements resolution logic (knows Java rules)
- FIR validates candidates (knows what types exist)

CRITICAL UNDERSTANDING:

Java resolution order (JLS):
1. Types in current compilation unit (handled by `classifier`)
2. Single-type imports (handled by `classifierQualifiedName`)
3. Types in current package (handled by `classifier` via `localScope`)
4. Star imports and java.lang.* (TO BE IMPLEMENTED)

If a name appears in multiple star imports → AMBIGUOUS (compile error)

IMPLEMENTATION STEPS:

### Phase 1: Add Interface Methods

1. READ javaTypes.kt:
   - File: `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt`
   - Find `interface JavaClassifierType`

2. ADD TWO MEMBERS to JavaClassifierType:
   ```kotlin
   /**
    * Whether this type is already resolved.
    * Default: true (PSI/javac-wrapper are always resolved)
    */
   val isResolved: Boolean
       get() = true
   
   /**
    * Resolves unresolved simple type names using import context.
    * 
    * @param tryResolve Lambda that validates if a fully qualified name exists
    * @return Resolved FQN, or null if not found/ambiguous
    */
   fun resolve(tryResolve: (String) -> Boolean): String? = null
   ```

3. VERIFY NO COMPILATION ERRORS:
   - Build should succeed (default implementations provided)
   - PSI/javac-wrapper implementations unchanged

### Phase 2: Implement in java-direct

4. OPEN JavaClassifierTypeOverAst.kt:
   - File: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/ast/types/JavaClassifierTypeOverAst.kt`

5. ADD isResolved PROPERTY:
   ```kotlin
   override val isResolved: Boolean
       get() {
           val typeName = node.text
           // Resolved if local, fully qualified, or in single-type imports
           return classifier != null 
               || typeName.contains('.')
               || imports?.simpleImports?.containsKey(typeName) == true
       }
   ```

6. IMPLEMENT resolve() METHOD:
   ```kotlin
   override fun resolve(tryResolve: (String) -> Boolean): String? {
       val simpleName = node.text
       
       // 1. Try java.lang.* (automatic import per JLS §7.5.5)
       val javaLangFqn = "java.lang.$simpleName"
       if (tryResolve(javaLangFqn)) {
           return javaLangFqn
       }
       
       // 2. Try explicit star imports in order (JLS §7.5.2)
       val starImports = imports?.starImports ?: emptyList()
       var foundFqn: String? = null
       
       for (packageFqName in starImports) {
           val candidateFqn = "${packageFqName.asString()}.$simpleName"
           if (tryResolve(candidateFqn)) {
               if (foundFqn != null) {
                   // Found in multiple packages - ambiguous!
                   // Return null to signal ambiguity
                   return null
               }
               foundFqn = candidateFqn
           }
       }
       
       return foundFqn  // null if not found
   }
   ```

7. VERIFY JavaImports has starImports:
   - Check that `imports?.starImports` is available
   - If not, ensure Iteration 3 properly stores star imports

### Phase 3: Use in FIR

8. OPEN JavaTypeConversion.kt:
   - File: `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`
   - Find `toConeKotlinTypeForFlexibleBound()` function (~line 183)

9. FIND THE `null ->` BRANCH (around line 248-251):
   ```kotlin
   null -> {
       val classId = ClassId.topLevel(FqName(this.classifierQualifiedName))
       classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
   }
   ```

10. REPLACE WITH RESOLUTION LOGIC:
   ```kotlin
   null -> {
       val qualifiedName = this.classifierQualifiedName
       
       val classId = if (!isResolved && !qualifiedName.contains('.')) {
           // Unresolved simple name - use callback
           resolveSimpleName(qualifiedName, this, session, source)
       } else {
           // Already resolved or fully qualified
           ClassId.topLevel(FqName(qualifiedName))
       }
       
       classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
   }
   ```

11. ADD HELPER FUNCTION (before or after toConeKotlinTypeForFlexibleBound):
   ```kotlin
   private fun resolveSimpleName(
       simpleName: String,
       javaType: JavaClassifierType,
       session: FirSession,
       source: KtSourceElement?
   ): ClassId {
       // Ask JavaModel to resolve using FIR's symbol provider
       val resolvedFqn = javaType.resolve { candidateFqn ->
           val classId = ClassId.topLevel(FqName(candidateFqn))
           session.symbolProvider.getClassLikeSymbolByClassId(classId) != null
       }
       
       return when {
           resolvedFqn != null -> {
               // Successfully resolved
               ClassId.topLevel(FqName(resolvedFqn))
           }
           else -> {
               // Not found or ambiguous - fall back
               ClassId.topLevel(FqName(simpleName))
           }
       }
   }
   ```

### Phase 4: Test and Validate

12. BUILD THE PROJECT:
   ```bash
   ./gradlew :compiler:java-direct:build
   ```

13. RUN BOX TESTS:
   ```bash
   ./gradlew :compiler:tests-for-compiler-generator:test \
     --tests "org.jetbrains.kotlin.test.runners.codegen.BlackBoxCodegenForJavaDirectSuppressionTestGenerated" \
     -q
   ```

14. ANALYZE RESULTS:
   - Count passing tests (expect ~120-130 out of 138)
   - Identify patterns in remaining failures
   - Check if java.lang types now resolve (Object, String, etc.)
   - Check if star-imported types resolve (List, ArrayList, etc.)

15. TEST SPECIFIC SCENARIOS:
   - Create small test with `Object obj;` - should resolve to java.lang.Object
   - Create test with `import java.util.*; List list;` - should resolve to java.util.List
   - Create test with ambiguous import - should handle gracefully

16. UPDATE ITERATION_RESULTS.md:
   - Document test results
   - List types that now resolve
   - List remaining failure categories
   - Estimate completion percentage

DELIVERABLE:
- Modified `javaTypes.kt` with new interface members
- Enhanced `JavaClassifierTypeOverAst.kt` with isResolved and resolve()
- Modified `JavaTypeConversion.kt` with resolution callback usage
- Box test results showing ~87-94% pass rate
- Analysis of remaining failures

VALIDATION CHECKLIST:
- [ ] Interface compiles with default implementations
- [ ] java-direct implements isResolved correctly
- [ ] java-direct implements resolve() with java.lang check
- [ ] java-direct implements resolve() with star import iteration
- [ ] FIR calls resolve() when isResolved == false
- [ ] Box tests improve from 11/138 to ~120-130/138
- [ ] java.lang types resolve (Object, String, Integer, etc.)
- [ ] Star-imported types resolve (List, ArrayList, etc.)

DEBUGGING TIPS:
- If resolve() never called: Check isResolved logic
- If wrong types resolved: Check resolution order (java.lang first, then star imports)
- If ambiguity not detected: Verify foundFqn != null check
- If tests still fail: Check if failures are resolution or other issues (generics, etc.)

CONFIRMATION REQUIRED: Confirm understanding of callback approach before starting.

---

## Iteration 5: Basic Type Arguments Parsing

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md` section 3.3.

### Prompt

---
TASK: Find ONE test failing on type arguments, fix it, measure impact

CONTEXT:
**Current Status: 30/138 (21.7%) tests passing**

Recent improvements:
- ✅ Parameters parsing (+18 tests)
- ✅ Java-to-Kotlin mapping
- ✅ Raw type name stripping (foundation laid)

**Hypothesis**: Many remaining failures likely involve generics (`List<String>`, `Map<K,V>`), since `typeArguments` returns empty list.

**Incremental Approach**: 
1. Find ONE test failing because of missing type arguments
2. Make THAT test pass
3. Count how many similar tests also pass

CRITICAL UNDERSTANDING:

Many Java interop tests use generics:
```java
List<String> list;              // Simple type argument
Map<String, Integer> map;       // Multiple type arguments
ArrayList<Object> objects;      // Type argument with java.lang type
```

Currently these parse but `JavaClassifierType.typeArguments` returns empty, causing type mismatches in FIR.

FOCUSED APPROACH STEPS:

### Step 0: Find ONE Representative Test (REQUIRED FIRST)

**Before implementing anything:**

```bash
# Run full suite to identify tests that might need generics
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q 2>&1 | tee test_output.txt

# Look for type mismatch errors that might involve generics
grep -B 3 "TYPE_MISMATCH\|RETURN_TYPE_MISMATCH" test_output.txt | grep "testData" | head -10
```

**Manual inspection:**
- Look at 3-5 candidate tests
- Find the simplest one that uses generic types
- Read its test data file
- Verify it's actually failing on type arguments (not something else)

**Select ONE test and document:**
```
Selected test: testXXXXX
Reason: Uses List<String>, only 30 lines, clear error about type argument mismatch
Expected fix: Parse REFERENCE_PARAMETER_LIST to extract <String>
```

**STOP and get confirmation before proceeding.**

---

IMPLEMENTATION STEPS (ONLY AFTER TEST SELECTION):

### Phase 1: Create Unit Test for YOUR Selected Test

1. EXTRACT MINIMAL CASE from the selected box test:
   ```kotlin
   @Test
   fun testYourSpecificCase() {
       // Copy the Java code from the box test you selected
       val source = """
           // Paste minimal Java code here
       """.trimIndent()
       
       // Parse and reproduce the EXACT issue
       val javaClass = parseJavaClass(source, "ClassName")
       val field = javaClass.fields.first()
       val fieldType = field.type as JavaClassifierType
       
       // This assertion will fail - that's the issue to fix
       assertEquals(1, fieldType.typeArguments.size)
   }
   ```

2. RUN ONLY THIS UNIT TEST:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaParsingTest.testYourSpecificCase" -q
   ```

3. VERIFY IT FAILS WITH EXPECTED ERROR:
   - Should fail because `typeArguments` returns empty
   - Make sure you're reproducing the RIGHT issue

### Phase 2: Investigate AST Structure for THIS Test

4. ADD DEBUG TEST to understand AST:
   ```kotlin
   @Test
   fun testDebugTypeArgumentsAST() {
       val source = """ /* same as above */ """.trimIndent()
       val parsed = KmpJavaParser.parse(source)
       
       // Print AST structure
       printASTTree(parsed)  // or use debugger to inspect
       
       // Find TYPE node with type arguments
       // Document what you find
   }
   ```

5. DOCUMENT AST STRUCTURE:
   - Where are type arguments in the tree?
   - What node type? `REFERENCE_PARAMETER_LIST`? `TYPE_ARGUMENT_LIST`?
   - How to extract them?

### Phase 3: Implement for THIS Test

6. FIX JavaClassifierTypeOverAst BASED ON YOUR FINDINGS:
   ```kotlin
   @Test
   fun testSimpleTypeArguments() {
       val source = """
           import java.util.List;
           class MyClass {
               public List<String> items;
               public List<Object> objects;
           }
       """.trimIndent()
       
       // Parse class, get field types
       val items = javaClass.fields.find { it.name.asString() == "items" }
       val itemsType = items.type as JavaClassifierType
       
       // Verify type arguments parsed
       assertEquals(1, itemsType.typeArguments.size)
       val arg = itemsType.typeArguments[0]
       assertFalse(arg.isWildcard)  // Not a wildcard
       // Verify it's String type
   }
   
   @Test
   fun testMultipleTypeArguments() {
       val source = """
           import java.util.Map;
           class MyClass {
               public Map<String, Integer> map;
           }
       """.trimIndent()
       
       // Verify 2 type arguments parsed
       val mapType = ... as JavaClassifierType
       assertEquals(2, mapType.typeArguments.size)
   }
   ```

4. MODIFY JavaClassifierTypeOverAst:
   - Add `TYPE_ARGUMENT_LIST` parsing
   - For each `TYPE_ARGUMENT`:
     - Extract `JAVA_CODE_REFERENCE` (the type)
     - Create `JavaType` recursively (reuse `createJavaType()`)
     - Wrap in `JavaTypeArgument` (check existing implementations)
   - Handle empty case: `List` (raw type) → empty typeArguments
   - Handle present case: `List<String>` → 1 type argument

5. IMPLEMENT JavaTypeArgument:
   - Check if `JavaTypeArgument` interface exists
   - Likely needs: `type: JavaType` property
   - For now: assume NOT wildcard (simple case)
   - Reference PSI or javac-wrapper implementation

### Phase 3: Integration

6. UPDATE RELATED CODE:
   - Ensure `createJavaType()` can handle recursive calls
   - Pass `localScope`, `imports` to nested type arguments
   - Verify no infinite recursion (type parameters referring to themselves)

7. HANDLE COMMON PATTERNS:
   - `List<String>`: String should resolve via java.lang
   - `List<Object>`: Object should resolve via java.lang
   - `ArrayList<String>`: ArrayList should resolve via imports
   - `Map<String, Integer>`: Both arguments should resolve

### Phase 4: Test and Validate

8. RUN YOUR UNIT TEST:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaParsingTest.testYourSpecificCase" -q
   ```
   - Should now PASS

9. RUN THE ONE BOX TEST YOU SELECTED:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testYourSelectedTest" -q
   ```
   - Should now PASS
   - If it doesn't pass, debug further - maybe type arguments weren't the only issue

10. RUN ~10 SIMILAR TESTS:
   ```bash
   # Find tests likely using similar generics
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.test*List*" -q
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.test*Map*" -q
   ```
   - Count how many more pass
   - Document which ones still fail and why

11. RUN FULL SUITE (measurement only):
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q 2>&1 | tee new_test_output.txt
   grep "tests completed" new_test_output.txt
   ```
   - Record new pass rate (e.g., 30/138 → 35/138)

12. UPDATE ITERATION_RESULTS.md:
   - Document your selected test
   - Document why it was failing (concrete root cause)
   - Document the fix
   - Document impact: "5 additional tests pass, all using List<String> or Map<K,V>"
   - Document remaining issues in the similar tests that still fail

DELIVERABLE:
- `JavaClassifierTypeOverAst.typeArguments` returns parsed list
- Unit tests for simple type arguments
- Box test improvement (target: +10-20 tests)
- Analysis of remaining failures

SCOPE LIMITATIONS (For This Iteration):
- ❌ **No wildcards yet**: `? extends Foo`, `? super Bar` - defer to Iteration 6
- ❌ **No complex bounds**: `<T extends Comparable<T>>` - defer to Iteration 7
- ❌ **No variance annotations**: @Nullable, @NotNull on type args - defer
- ✅ **Yes simple types**: `List<String>`, `Map<String, Integer>`
- ✅ **Yes nested**: `List<List<String>>` (recursive createJavaType)
- ✅ **Yes raw detection**: `List` vs `List<String>`

VALIDATION CHECKLIST:
- [ ] TYPE_ARGUMENT_LIST node found in AST
- [ ] Type arguments extracted and counted correctly
- [ ] Each type argument creates JavaType recursively
- [ ] Imports/localScope passed to nested types
- [ ] Raw types (no <>) return empty typeArguments
- [ ] Parameterized types return non-empty typeArguments
- [ ] Unit tests verify List<String> parses 1 argument
- [ ] Unit tests verify Map<K,V> parses 2 arguments
- [ ] Box tests improve by 10-20 tests

DEBUGGING TIPS:
- If no TYPE_ARGUMENT_LIST: Check AST structure, may be named differently
- If infinite recursion: Add depth limit or visited set
- If resolution fails: Ensure imports propagated to nested types
- If tests don't improve: Check FIR uses typeArguments (may need FIR changes)

CONFIRMATION REQUIRED: Confirm understanding before starting. Check if TYPE_ARGUMENT_LIST exists in AST.

---

## Iteration 6: Wildcards and Upper Bounds

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md` section 3.3.

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

## Iteration 7: Array Types

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md`.

### Prompt

---
TASK: Implement proper array type support

CONTEXT:
**Expected Status After Iteration 6: ~40-50/138 (29-36%) tests passing**

Java arrays are common in test code and API boundaries. Proper array type handling may unlock additional tests.

IMPLEMENTATION STEPS:

1. VERIFY CURRENT STATE:
   - Check if `isArray()` is implemented in JavaTypeOverAst
   - Verify component type extraction works
   - Test primitive arrays vs reference arrays

2. IMPLEMENT ARRAY TYPE PARSING:
   - Parse array dimensions (`String[]`, `int[][]`)
   - Return correct component types
   - Handle multi-dimensional arrays

3. FIR INTEGRATION:
   - Ensure FIR creates correct array types
   - Verify kotlin.Array vs primitive arrays mapping
   - Test array type variance

4. CREATE TESTS:
   ```kotlin
   @Test
   fun testArrayTypes() {
       val source = """
           class ArrayTest {
               public String[] names;
               public int[][] matrix;
           }
       """.trimIndent()
       // Verify array type parsing
   }
   ```

5. RUN BOX TESTS:
   - Expect +5-10 tests with array usage

6. TARGET: 45-60/138 (33-43%) pass rate

DELIVERABLE:
- Array type support
- Multi-dimensional arrays
- Test improvements

CONFIRMATION REQUIRED: Assess after Iteration 6 if arrays are blocking tests.

---

## Iteration 8: Lower Bounds and Complex Generics

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md`.

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

## Iteration 9: Annotations and Nullability

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md` section 3.5.

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

## Iteration 10: Inner Classes and Nested Types

**Reference**: See `AGENT_INSTRUCTIONS.md` and `IMPLEMENTATION_PLAN.md`.

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

## Iteration 11: Annotation Support

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

## Iteration 12: Error Handling and Diagnostics

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

## Iteration 13: Performance and Caching

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

## Iteration 14: Final Validation and Documentation

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
