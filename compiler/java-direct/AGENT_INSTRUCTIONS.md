# Java-Direct: Agent Instructions

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Key files**: `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`, `JavaResolutionContext.kt`, `JavaClassFinderOverAstImpl.kt`

---

## Ground Rules

### Mandatory
- **Use JetBrains MCP tools** for all project file operations (see `.ai/guidelines.md`)
- **NEVER create git commits** — all changes must be reviewed by the user first
- **FIR terminology**: `simpleImports`/`starImports` (NOT singleType/onDemand)
- **Update ITERATION_RESULTS.md** after each iteration
- **Check files with `get_file_problems`** after changes
- **Run PSI tests after FIR changes** — see "Shared Files" section below

### Code Quality
- **No obvious comments** — comment only "why", never "what"
- **Minimal code** — solve the problem, don't over-engineer
- **Lazy evaluation** — avoid eager computation
- **Prefer callbacks over hardcoded lists** — see `implDocs/ARCHITECTURE.md`

### Default Assumptions
- **When a fix doesn't work**: assume the implementation is wrong, not that there's an external/systemic blocker. Debug further before concluding the issue is outside java-direct.
- **Baseline diffs are real bugs** until proven otherwise by verifying the same test passes with PSI-based FIR.

---

## Pre-Implementation Checklist

**MANDATORY before writing any fix:**

- [ ] **Verified AST node names** by checking java-syntax-jvm sources (token names often differ from constant names — e.g. `SEALED` not `SEALED_KEYWORD`, `RECORD` not `RECORD_KEYWORD`; check `SyntaxElementType("...")` string in JavaSyntaxTokenType.kt)
- [ ] **Checked reference implementation** in javac-wrapper or PSI (see `implDocs/ARCHITECTURE.md` for paths)
- [ ] **Confirmed root cause** by debugging 2-3 representative failing tests
- [ ] **Verified correct test class name** before running full suite (use wildcards below)

---

## Shared vs Java-Direct Files

**CRITICAL**: Some files are shared with PSI-based class finders.

**Java-Direct specific** (safe to modify): `compiler/java-direct/src/**`

**Shared FIR files** (modify with caution):
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt`
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`
- `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt`
- `core/compiler.common.jvm/src/.../load/java/structure/*.kt`

**After modifying shared files**, always run PSI regression tests:
```bash
./gradlew :compiler:fir:fir-jvm:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" -q
```

---

## Test Commands

```bash
# Box tests (~1167 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" -q 2>&1 | tee test_output.txt

# Phased/diagnostic tests (~1442 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" -q 2>&1 | tee test_output.txt

# Single test (use wildcard to avoid nested class path issues)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated.*testSpecificName*" -q
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated.*testSpecificName*" -q

# PSI regression (after shared FIR file changes)
./gradlew :compiler:fir:fir-jvm:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" -q

# Verify a test is java-direct specific (not a general FIR issue)
./gradlew :compiler:fir:fir-jvm:test --tests "FirLightTreeBlackBoxCodegenTestGenerated.*testSpecificName*" -q
```

---

## What NOT to Do

- **Don't hardcode lists** for resolution/filtering — use callback pattern
- **Don't modify shared FIR files** without running PSI regression tests
- **Don't assume AST token names** — always dump and verify via exception debugging
- **Don't estimate fix counts without debugging** — verify root cause with 2-3 tests first
- **Don't implement partial interfaces** — e.g., `JavaAnnotationArgument` requires value subinterfaces, not just `name`
- **Don't create git commits** — user reviews all changes first

---

## Detailed Reference Documents

| Document | When to consult |
|----------|----------------|
| `implDocs/ARCHITECTURE.md` | Callback patterns, key files, reference implementations, common fixes |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Debugging techniques, categorization script, AST inspection |
| `FIXING_ITERATIONS.md` | Current state, remaining categories, next iteration plans |
| `ITERATION_RESULTS.md` | History of what was tried and learned |
| `IMPLEMENTATION_PLAN.md` | High-level architecture overview |
| `implDocs/archive/` | Archived iteration details (deep context recovery only) |

---

*Last updated: 2026-03-13 (restructured per PROCESS_ANALYSIS_V2.md recommendation 6)*
