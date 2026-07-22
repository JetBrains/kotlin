# Focus Strategy for Agents

## The Core Problem

Agents were struggling because they:
- ❌ Ran all 138 tests at once
- ❌ Tried to categorize all failure patterns
- ❌ Made speculative fixes hoping to improve aggregate metrics
- ❌ Couldn't debug when fixes didn't work

## The Solution: Test-Driven Focus

### The One Test Rule

**Pick ONE concrete failing test. Make it pass. Measure impact.**

### Why This Works

1. **Concrete debugging**: You can trace execution for one specific case
2. **Clear success criteria**: The test either passes or it doesn't
3. **Fast feedback loop**: Minutes instead of hours
4. **Measurable impact**: Count how many similar tests also pass
5. **Clear next steps**: If impact is small, pick a different test

## The Focused Workflow

### Phase 1: Statistics (5 minutes)

Run full suite ONCE to get error frequencies:

```bash
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q 2>&1 | tee full_output.txt
grep "ERROR_TYPE" full_output.txt | sort | uniq -c | sort -rn
```

Pick the most frequent error type.

### Phase 2: Selection (10 minutes)

Find the SIMPLEST test with that error:
- Shortest code
- Fewest features (no generics, nested classes, etc.)
- Most focused scenario

**Write it down**: "I selected testAbstractMethodsOfAny because it has the most common error (UNRESOLVED_REFERENCE '<init>'), has only 20 lines, and uses no complex features."

### Phase 3: Deep Dive (no time limit)

Work ONLY on that test until you understand it completely:

```bash
# Run ONLY this test repeatedly
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testAbstractMethodsOfAny" -q
```

- Read test data file
- Understand what it tests
- Trace execution
- Add logging/breakpoints
- Find exact failure point
- Form concrete hypothesis

### Phase 4: Reproduce (30 minutes)

Create unit test with minimal case:

```kotlin
@Test
fun testReproduceIssue() {
    val source = """ /* minimal Java code */ """.trimIndent()
    val javaClass = parseJavaClass(source, "ClassName")
    
    // The assertion that exposes the bug
    assertTrue(javaClass.hasDefaultConstructor())  // fails but should pass
}
```

### Phase 5: Fix (varies)

Implement the fix:
- Change specific code
- Run unit test → passes
- Run the ONE box test → passes

### Phase 6: Measure Impact (10 minutes)

```bash
# Run similar tests
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.test*Inheritance*" -q

# Run full suite
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q
```

Document:
- Selected test: passes ✅
- Similar tests: 12 more pass
- Full suite: 30/138 → 42/138 (40% improvement)

### Phase 7: Iterate

If impact is good (10+ tests): Document and continue to next issue.

If impact is small (1-2 tests): Maybe pick a different test representing a more common pattern.

## Example: Iteration 4 Success

**What was done in Iteration 4 that worked:**

1. ✅ Identified most common error: `UNRESOLVED_REFERENCE: '<init>'` (128 occurrences)
2. ✅ Created focused unit test: `testDefaultConstructor()`
3. ✅ Found root cause: `hasDefaultConstructor()` hardcoded to `false`
4. ✅ Made one-line fix
5. ✅ Result: ALL 128 tests with `<init>` error changed error patterns

**What happened in Iteration 5 that didn't work as well:**

1. ⚠️ Implemented type arguments (correct implementation)
2. ⚠️ But ran all tests without first verifying ONE test needed this
3. ⚠️ Result: 0 improvement (30/138 → 30/138)
4. ⚠️ Why? Type arguments weren't the blocker for remaining tests

**What should have happened in Iteration 5:**

1. Run tests, find most common error (e.g., `TYPE_MISMATCH with List`)
2. Find simplest test with that error
3. Verify it's actually failing on type arguments
4. Make THAT test pass
5. Measure: if it helps 10+ tests → good, if 0 tests → wrong hypothesis

## Anti-Patterns to Avoid

### ❌ The "Shotgun Approach"
```
Agent: "I'll implement type arguments, wildcards, and bounds"
[Implements 500 lines of code]
[Runs all tests]
Result: No improvement
Problem: Don't know which part was needed
```

### ❌ The "Analysis Paralysis"
```
Agent: "Let me categorize all 108 failures..."
[Spends 2 hours reading test files]
[Creates 15 categories]
[Still doesn't know what to fix first]
```

### ❌ The "Speculative Fix"
```
Agent: "Many tests use generics, so I'll implement typeArguments"
[Implements feature]
Result: 0 tests pass
Problem: Generics weren't actually the blocker
```

## The Right Pattern: ✅ Test-Driven Focus

```
Agent: "Running suite to find most frequent error..."
[Finds TYPE_MISMATCH appears 50 times]

Agent: "Finding simplest TYPE_MISMATCH test..."
[Identifies testMethodReturnType - 15 lines, simple]

Agent: "Running ONLY testMethodReturnType..."
[Error: expected kotlin.String, got java.lang.String]

Agent: "Creating unit test to reproduce..."
[Creates testJavaToKotlinMapping unit test]
[Test fails with same error]

Agent: "Root cause: JavaToKotlinClassMap not applied in JavaTypeConversion"
[Makes 5-line fix]

Agent: "Testing unit test..."
[Passes ✅]

Agent: "Testing testMethodReturnType..."
[Passes ✅]

Agent: "Testing all *ReturnType* tests..."
[8 more pass ✅]

Agent: "Running full suite..."
[30/138 → 38/138, documents in ITERATION_RESULTS.md]
```

## Quick Reference Card

When starting an iteration:

1. ✅ Get error statistics (5 min)
2. ✅ Pick most frequent error
3. ✅ Find simplest test with that error
4. ✅ **STOP and document your selection**
5. ✅ Get approval before coding
6. ✅ Work on that ONE test only
7. ✅ Make it pass
8. ✅ Measure impact on similar tests
9. ✅ Measure impact on full suite
10. ✅ Document in ITERATION_RESULTS.md

Remember: **One test at a time. Make it pass. Measure. Repeat.**
