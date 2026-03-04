# Investigation Techniques for Java-Direct Test Failures

This document captures the systematic approaches used to assess, classify, and debug test failures in the java-direct module.

---

## Phase 1: Error Classification

### 1.1 Running Tests and Capturing Results

```bash
# Run all box tests
./gradlew :compiler:java-direct:test --tests "org.jetbrains.kotlin.java.direct.JavaUsingAstLegacyBoxTestGenerated*" -q

# Run specific test subset
./gradlew :compiler:java-direct:test --tests "org.jetbrains.kotlin.java.direct.JavaUsingAstLegacyBoxTestGenerated\$NotNullAssertions*" -q
```

**Output**: Test results are stored in:
- `compiler/java-direct/build/test-results/test/*.xml` (JUnit XML)
- `compiler/java-direct/build/reports/tests/test/` (HTML reports)

### 1.2 Extracting Failure Categories

Use Python to parse JUnit XML and categorize failures:

```python
import xml.etree.ElementTree as ET
import glob
from collections import defaultdict

results = []
for xml_file in glob.glob("compiler/java-direct/build/test-results/test/*.xml"):
    tree = ET.parse(xml_file)
    for tc in tree.findall('.//testcase'):
        failure = tc.find('failure')
        if failure is not None:
            name = tc.get('name', '')
            msg = (failure.get('message', '') or failure.text or '')[:300]
            
            # Categorize by error pattern
            if 'MISSING_DEPENDENCY_CLASS' in msg:
                etype = 'MISSING_DEPENDENCY_CLASS'
            elif 'NOTHING_TO_OVERRIDE' in msg:
                etype = 'NOTHING_TO_OVERRIDE'
            elif 'ARGUMENT_TYPE_MISMATCH' in msg:
                etype = 'ARGUMENT_TYPE_MISMATCH'
            elif 'CANNOT_INFER_PARAMETER_TYPE' in msg:
                etype = 'CANNOT_INFER_PARAMETER_TYPE'
            elif 'UNRESOLVED_REFERENCE' in msg:
                etype = 'UNRESOLVED_REFERENCE'
            elif 'AbstractMethodError' in msg:
                etype = 'AbstractMethodError'
            elif 'NoSuchMethodError' in msg:
                etype = 'NoSuchMethodError'
            elif 'NONE_APPLICABLE' in msg:
                etype = 'NONE_APPLICABLE'
            elif 'should throw' in msg or 'NullPointerException expected' in msg:
                etype = 'NPE_ASSERTION'
            else:
                etype = 'OTHER'
            results.append((etype, name, msg[:100]))

# Group by error type
by_type = defaultdict(list)
for etype, name, msg in results:
    by_type[etype].append(name)

for etype, tests in sorted(by_type.items(), key=lambda x: -len(x[1])):
    print(f"\n=== {etype} ({len(tests)} tests) ===")
    for t in sorted(tests):
        print(f"  - {t}")
```

### 1.3 Quick Shell-Based Categorization

```bash
# List all failing test names
grep -B1 "<failure" compiler/java-direct/build/test-results/test/*.xml | \
  grep -oE 'name="test[^"]*"' | sed 's/name="//;s/"$//' | sort

# Count error types from HTML reports
grep -E "(MISSING_DEPENDENCY|NOTHING_TO_OVERRIDE|ARGUMENT_TYPE_MISMATCH)" \
  compiler/java-direct/build/reports/tests/test/classes/*.html | \
  grep -oE '(MISSING_DEPENDENCY_CLASS|NOTHING_TO_OVERRIDE|ARGUMENT_TYPE_MISMATCH)' | \
  sort | uniq -c | sort -rn
```

---

## Phase 2: Understanding Test Structure

### 2.1 Locating Test Data Files

Box tests use test data files in `compiler/testData/codegen/box/`. Find the test data:

```bash
# Find test data for a specific test
find compiler/testData/codegen/box -name "*FunctionWithBigArity*" -o -name "*functionWithBigArity*"

# Or search by test class path pattern
ls compiler/testData/codegen/box/javaInterop/notNullAssertions/
```

### 2.2 Reading Test Data Structure

Test data files typically contain:
1. **Kotlin source** (`.kt`) - the code being tested
2. **Java source** (`.java`) - Java code that Kotlin interacts with
3. **Directives** - special comments like `// FILE:`, `// TARGET_BACKEND:`

```kotlin
// FILE: J.java
public class J {
    public String foo(String[] a) { return a[0]; }
}

// FILE: test.kt
fun box(): String {
    return J().foo(arrayOf("OK"))
}
```

---

## Phase 3: Deep Debugging with Exception-Based Tracing

### 3.1 Why Exception-Based Debugging?

**Problem**: Standard `println()` or logging doesn't appear in Gradle test output.

**Solution**: Throw exceptions with debug information. The exception message appears in test failure output.

### 3.2 Basic Pattern

```kotlin
// In the code path you want to inspect:
if (someCondition) {
    throw IllegalStateException(
        "DEBUG: variable=$variable, " +
        "otherValue=$otherValue"
    )
}
```

### 3.3 AST Inspection Pattern

The java-direct module parses Java using KMP parser into AST nodes. To understand AST structure:

```kotlin
// Add to JavaSyntaxNode or create extension:
fun JavaSyntaxNode.dump(indent: String = ""): String {
    val sb = StringBuilder()
    sb.append("$indent${type}: ${text.take(50)}\n")
    for (child in children) {
        sb.append(child.dump("$indent  "))
    }
    return sb.toString()
}

// Then in the code you're debugging:
throw IllegalStateException(
    "DEBUG AST:\n${node.dump()}"
)
```

### 3.4 Conditional Triggering

To debug specific cases without breaking all tests:

```kotlin
// Trigger only for specific text patterns
if (node.text.contains("FunctionN") || node.text.contains("kotlin.jvm")) {
    throw IllegalStateException("DEBUG: ${node.dump()}")
}

// Trigger only for specific type patterns
if (typeNode.text.contains("[")) {  // Array types
    throw IllegalStateException("DEBUG ARRAY: ${typeNode.dump()}")
}
```

### 3.5 Example: Discovering ERROR_ELEMENT Import Issue

```kotlin
// In JavaClassFinderOverAstImpl.parseTopLevelClassFromFile():
private fun parseTopLevelClassFromFile(file: VirtualFile, className: String): JavaClass? {
    val text = String(file.contentsToByteArray())
    val root = JavaParser.parse(text)
    
    // Debug: dump the import list to see structure
    val importList = root.findChildByType("IMPORT_LIST")
    throw IllegalStateException(
        "DEBUG parseTopLevelClassFromFile:\n" +
        "className=$className\n" +
        "importList children:\n${importList?.children?.joinToString("\n") { 
            "  ${it.type}: ${it.text.take(80)}" 
        }}"
    )
    // ...
}
```

This revealed:
```
importList children:
  IMPORT_STATEMENT: import java.util.List;
  ERROR_ELEMENT: import kotlin.jvm.functions.FunctionN;   // <-- Problem!
  IMPORT_STATEMENT: import java.util.Map;
```

### 3.6 Tracing Type Resolution

```kotlin
// In JavaTypeConversion.kt, toFirJavaTypeRef():
val classId = computeClassId(classifierQualifiedName)
val symbol = classId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) }

// Debug insertion:
if (classifierQualifiedName.contains("Function") || symbol == null) {
    throw IllegalStateException(
        "DEBUG type resolution:\n" +
        "classifierQualifiedName=$classifierQualifiedName\n" +
        "classId=$classId\n" +
        "symbol=$symbol\n" +
        "isResolved=${(javaType as? JavaClassifierType)?.isResolved}"
    )
}
```

---

## Phase 4: Comparing with Working Implementation

### 4.1 Identifying the Reference Implementation

The java-direct module is an alternative to the PSI-based Java model. Compare with:
- `compiler/frontend.java/src/org/jetbrains/kotlin/load/java/structure/impl/` (PSI-based)
- Specifically: `JavaClassifierTypeImpl.kt`, `JavaClassImpl.kt`, etc.

### 4.2 Comparison Pattern

```kotlin
// In PSI implementation (working):
class JavaClassifierTypeImpl : JavaClassifierType {
    override val classifier: JavaClassifier?
        get() = psi.resolveGenerics().element?.let { ... }  // Uses PSI resolution
}

// In java-direct (to fix):
class JavaClassifierTypeOverAst : JavaClassifierType {
    override val classifier: JavaClassifier?
        get() = resolutionContext.findLocalClass(...)  // Only checks local scope!
}
```

### 4.3 Scope Analysis

When class resolution fails, check what scopes are being searched:

```kotlin
// Debug scope contents
throw IllegalStateException(
    "DEBUG scope check:\n" +
    "Looking for: $simpleName\n" +
    "Local classes: ${resolutionContext.localClasses.keys}\n" +
    "Simple imports: ${resolutionContext.simpleImports}\n" +
    "Star imports: ${resolutionContext.starImports}"
)
```

---

## Phase 5: Iterative Fix Verification

### 5.1 Before/After Test Counts

Always record test counts before and after changes:

```bash
# Before fix
./gradlew :compiler:java-direct:test --tests "...BoxTestGenerated*" -q 2>&1 | grep "tests completed"
# Output: 138 tests completed, 48 failed

# After fix
./gradlew :compiler:java-direct:test --tests "...BoxTestGenerated*" -q 2>&1 | grep "tests completed"
# Output: 138 tests completed, 42 failed  → 6 tests fixed!
```

### 5.2 Verifying Error Type Changes

A fix may not reduce failure count but change error types (progress!):

```
Before: MISSING_DEPENDENCY_CLASS: Cannot access class 'FunctionN'
After:  ARGUMENT_TYPE_MISMATCH: actual type is 'FunctionN<Any>'...
```

This proves the class is now being found; the remaining issue is different.

### 5.3 Single Test Verification

```bash
# Run single test for quick feedback
./gradlew :compiler:java-direct:test \
  --tests "org.jetbrains.kotlin.java.direct.JavaUsingAstLegacyBoxTestGenerated.testFunctionWithBigArity" -q
```

---

## Phase 6: Documentation Updates

After each investigation cycle, update:

1. **ITERATION_RESULTS.md** - What was found, what was fixed, test counts
2. **ITERATION_7_PROBLEM_ANALYSIS.md** - Detailed analysis of remaining issues
3. **This document** - New techniques discovered

### Template for Recording Findings

```markdown
## Finding: [Title]

**Symptom**: [Error message or behavior]

**Investigation**:
1. [Step taken]
2. [What was discovered]

**Root Cause**: [Explanation]

**Fix**: [Code change description]

**Verification**: [Test results before/after]
```

---

## Quick Reference: Common Debug Insertions

### Type Resolution
```kotlin
throw IllegalStateException("DEBUG type: qname=$classifierQualifiedName, resolved=$isResolved")
```

### AST Structure
```kotlin
throw IllegalStateException("DEBUG AST:\n${node.dump()}")
```

### Import Resolution
```kotlin
throw IllegalStateException("DEBUG import: simple=${simpleImports[name]}, stars=$starImports")
```

### Class Finding
```kotlin
throw IllegalStateException("DEBUG finder: looking for $classId, found=${symbol != null}")
```

---

## Lessons Learned

1. **Exception-based debugging is essential** - Only reliable way to see output in Gradle tests

2. **AST structure is not obvious** - Always dump the actual structure; don't assume

3. **Parser errors don't mean data loss** - ERROR_ELEMENT nodes often contain recoverable info

4. **Compare with working implementation** - PSI-based code is the reference

5. **Track error type changes** - Same failure count can hide progress

6. **Reserved words cause parser issues** - `kotlin` in imports triggers Java 9 module parsing

7. **Check input node type first** - Before calling `findChildByType()`, verify the node isn't already the target type
