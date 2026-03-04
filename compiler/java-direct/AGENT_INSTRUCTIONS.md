# Java-Direct: Agent Instructions

## Quick Reference

| Metric | Value |
|--------|-------|
| **Status** | Iteration 7 in progress |
| **Tests** | 90/138 passing (65.2%) |
| **Next focus** | Array types, type parameters, Kotlin class resolution |

**Key files**: `JavaClassFinderOverAstImpl.kt`, `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaImports.kt`

---

## Ground Rules

### Mandatory
- **Use JetBrains MCP tools** for all project file operations (see `.ai/guidelines.md`)
- **FIR terminology**: `simpleImports`/`starImports` (NOT singleType/onDemand)
- **Update ITERATION_RESULTS.md** after each iteration
- **Check files with `get_file_problems`** after changes

### Code Quality
- **No obvious comments** — comment only "why", never "what"
- **Minimal code** — solve the problem, don't over-engineer
- **Every fix needs a test** — unit test or box test must pass
- **Lazy evaluation** — avoid eager computation

---

## Iteration Workflow

### 1. Pick ONE Failing Test
```bash
# Get current failure statistics
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q 2>&1 | tee test_output.txt
grep -E "(FAIL|Exception)" test_output.txt | sort | uniq -c | sort -rn | head -10
```
Pick the **simplest** test with the **most common** error pattern.

### 2. Debug That Single Test
```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testSpecificName" -q
```
Use exception-based debugging (see below) to understand the failure.

### 3. Fix and Verify
1. Implement fix
2. Verify single test passes
3. Run related tests (10-20 with similar pattern)
4. Run full suite to measure progress

### 4. Document
Update `ITERATION_RESULTS.md` with findings, changes, and test counts.

---

## Debugging: Exception-Based Inspection

**Why**: println/logging doesn't appear in Gradle test output.

**Pattern**:
```kotlin
throw IllegalStateException("DEBUG: value='$value', context='$context'")
```

**Conditional** (debug specific cases only):
```kotlin
if (classId.shortClassName.asString() == "TargetClass") {
    throw IllegalStateException("DEBUG: classId=$classId, supertypes=${supertypes.size}")
}
```

Remove debug exceptions before committing.

### AST Structure Discovery

To understand how the KMP Java Parser represents a construct, use exception-based debugging with `node.dump()`:

```kotlin
// In the relevant getter (e.g., JavaValueParameterOverAst.type):
if (typeNode.text.contains("[")) {  // or other condition
    throw IllegalStateException(
        "DEBUG: typeNode.dump=\n${typeNode.dump()}"
    )
}
```

Example output for `String[]`:
```
TYPE: String[]
  TYPE: String
    JAVA_CODE_REFERENCE: String
      IDENTIFIER: String
  LBRACKET: [
  RBRACKET: ]
```

---

## Test Failure Analysis

### Categorizing Failures

Run full test suite and analyze XML results:

```bash
# Run all tests
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q 2>&1

# Parse results with Python
python3 << 'EOF'
import xml.etree.ElementTree as ET
import glob
from collections import defaultdict

results_dir = "compiler/java-direct/build/test-results/test"
failures = defaultdict(list)

for xml_file in glob.glob(f"{results_dir}/*.xml"):
    tree = ET.parse(xml_file)
    for tc in tree.findall('.//testcase'):
        failure = tc.find('failure')
        if failure is not None:
            name = tc.get('name').replace('()','')
            text = (failure.text or '') + (failure.get('message', '') or '')
            # Categorize by error pattern
            if 'MISSING_DEPENDENCY_CLASS' in text:
                failures['MISSING_DEPENDENCY_CLASS'].append(name)
            elif 'NOTHING_TO_OVERRIDE' in text:
                failures['NOTHING_TO_OVERRIDE'].append(name)
            # Add more patterns as needed...

for cat, tests in sorted(failures.items(), key=lambda x: -len(x[1])):
    print(f"\n=== {cat} ({len(tests)} tests) ===")
    for t in tests: print(f"  - {t}")
EOF
```

### Finding Test Data Files

```bash
# Find test data for a specific test
find compiler/testData/codegen/box/javaInterop -name "*testName*"
```

---

## Key Architecture Decisions

From iterations 1-6 (see `ITERATION_RESULTS.md` for details):

1. **Type resolution in FIR layer** — Java Model provides names via `classifierQualifiedName`, FIR resolves them
2. **Callback pattern for star imports** — `resolve(tryResolve: (String) -> Boolean)` in `JavaClassifierType`
3. **Hybrid finder** — `CombinedJavaClassFinder` tries sources first, falls back to binaries

---

## Key Files

| File | Purpose |
|------|---------|
| `JavaClassFinderOverAstImpl.kt` | Source class finder, file indexing |
| `JavaClassOverAst.kt` | Java class model |
| `JavaTypeOverAst.kt` | Type representations, `classifierQualifiedName` |
| `JavaImports.kt` | Import handling |
| `JavaMemberOverAst.kt` | Methods, fields, parameters |
| `JavaDirectComponentRegistrar.kt` | Plugin registration, hybrid finder setup |

**FIR integration** (reference, don't modify unless necessary):
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` — uses `resolve()` callback
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` — converts Java Model to FIR

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Circular FIR↔Java dependency | Use lazy evaluation |
| Wrong terminology | Use FIR names (simpleImports, starImports) |
| Using wrong tools | Use MCP tools, not Read/Edit/Grep |
| Eager evaluation | Make everything lazy |
| No tests | Every fix needs a passing test |

---

## Useful Commands

```bash
# Single test
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testName" -q

# All box tests
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q

# Build module
./gradlew :compiler:java-direct:build -q
```

---

## Related Documents

| Document | Purpose |
|----------|---------|
| `FIXING_ITERATIONS.md` | Current iteration plans and tasks |
| `IMPLEMENTATION_PLAN.md` | Architecture overview, type resolution design |
| `implDocs/ITERATION_7_PROBLEM_ANALYSIS.md` | Detailed problem analysis for iteration 7 |
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | Archived iteration 1-6 details |
| `../AGENTS.md` | Compiler architecture overview (K1/K2, FIR, IR, backends) |
| `../../.ai/guidelines.md` | **MANDATORY** - Project-wide coding guidelines |

---

## Remaining Work (48 failing tests)

Based on iteration 7 analysis (see `implDocs/ITERATION_7_PROBLEM_ANALYSIS.md`):

| Category | Count | Priority |
|----------|-------|----------|
| MISSING_DEPENDENCY_CLASS | 15 | Medium (complex) |
| Wrong NPE behavior | 6 | Low |
| NOTHING_TO_OVERRIDE (array types) | 5 | **High (quick win)** |
| NONE_APPLICABLE | 4 | Medium |
| CANNOT_INFER_PARAMETER_TYPE | 3 | Medium |
| Other | 15 | Varies |

---

*Last updated: 2026-03-04 (iteration 7 analysis)*
