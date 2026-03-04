# Java-Direct: Agent Instructions

## Quick Reference

| Metric | Value |
|--------|-------|
| **Status** | Iteration 6 complete |
| **Tests** | 90/138 passing (65.2%) |
| **Next focus** | Type parameters, generics, SAM lambdas |

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
| `ITERATION_RESULTS.md` | Progress history, key findings from iterations 1-6 |
| `IMPLEMENTATION_PLAN.md` | Architecture overview, type resolution design |
| `FIRSESSION_RESOLUTION_ANALYSIS.md` | Why type resolution happens in FIR, not Java Model |
| `../AGENTS.md` | Compiler architecture overview (K1/K2, FIR, IR, backends) |
| `../../.ai/guidelines.md` | **MANDATORY** - Project-wide coding guidelines |
| `../../.ai/testing.md` | Test infrastructure and conventions |

---

## Remaining Work (48 failing tests)

Likely causes based on iteration 6 analysis:
1. **Type parameters** (`T`, `U`) treated as class names
2. **Complex generics** (`? extends`, `? super`)
3. **SAM lambda inference** (`it` parameter)
4. **Other semantic issues**

---

*Last updated: 2026-03-03 (after iteration 6)*
