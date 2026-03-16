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
- **Run PSI tests after any shared file or test data changes** — see "Shared Files" section below
- **Keep JavaParsingTest green** — run after core changes to JavaTypeOverAst, JavaClassOverAst, JavaResolutionContext
- **Check `git diff` for unintended test data changes** after every test run — especially after using `-Pkotlin.test.update.test.data=true`

### Test Data Is Ground Truth
Test data files (`compiler/testData/`) reflect the **correct behavior as defined by javac and the PSI-based implementation**. They are shared between java-direct and PSI test runners.

**NEVER modify test data to make java-direct tests pass.** If java-direct produces a different result than what the test data expects:
- The java-direct implementation is wrong — fix it, or
- It is a known acceptable difference — document it in ITERATION_RESULTS.md and leave the test data unchanged

Modifying test data to match java-direct output will break the PSI-based tests, which represent the reference behavior.

### Code Quality
- **No obvious comments** — comment only "why", never "what"
- **Minimal code** — solve the problem, don't over-engineer
- **Lazy evaluation** — avoid eager computation
- **Prefer callbacks over hardcoded lists** — see `implDocs/ARCHITECTURE.md`

### Default Assumptions
- **When a fix doesn't work**: assume the implementation is wrong, not that there's an external/systemic blocker. Debug further before concluding the issue is outside java-direct.
- **Baseline diffs are real bugs** until proven otherwise by verifying the same test passes with PSI-based FIR.

---

## Preferred Approach: Reference-First Area Audit

**Before hunting individual test exceptions**, prefer a systematic area audit:

1. **Pick a code area** — e.g., member visibility, type parameters, annotation arguments
2. **Read the reference** — open the equivalent class in javac-wrapper (`compiler/javac-wrapper/src/.../wrappers/trees/TreeBasedField.kt`, `TreeBasedClass.kt`, `TreeBasedMethod.kt`) or PSI (`compiler/frontend.common.jvm/src/.../impl/JavaClassImpl.java`, `JavaMemberImpl.java`, `JavaFieldImpl.java`)
3. **Compare property by property** with the java-direct equivalent (`JavaClassOverAst.kt`, `JavaMemberOverAst.kt`, etc.)
4. **List ALL discrepancies** in one pass — don't stop at the first one
5. **Fix all discrepancies together** — this turns 3–4 exception-driven iterations into one targeted fix

**For any shared file change**, check upstream git first — the issue may already be solved differently there:
```bash
git show origin/master:<path/to/shared/file> | grep -A10 "relevantFunction"
```
This takes seconds vs. 30+ minutes of FIR2IR tracing.

---

## Pre-Implementation Checklist

**MANDATORY before writing any fix:**

- [ ] **Checked reference implementation** in javac-wrapper or PSI (see `implDocs/ARCHITECTURE.md` for paths)
- [ ] **For shared files: compared with `git show origin/master:...`** to see what upstream does
- [ ] **Verified AST node names** by checking java-syntax-jvm sources (token names often differ from constant names — e.g. `SEALED` not `SEALED_KEYWORD`, `RECORD` not `RECORD_KEYWORD`; check `SyntaxElementType("...")` string in JavaSyntaxTokenType.kt)
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

**After modifying shared files or test data**, always run PSI regression tests:
```bash
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" -q
```

---

## Test Commands

```bash
# Box tests (~1168 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" -q --rerun 2>&1 | tee /tmp/jd_test.txt

# Phased/diagnostic tests (~1442 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" -q --rerun 2>&1 | tee /tmp/jd_test.txt

# Both suites together (2610 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated" -q --rerun 2>&1 | tee /tmp/jd_test.txt

# Unit tests (40 tests - MUST stay green)
./gradlew :compiler:java-direct:test --tests "JavaParsingTest" -q

# Single test (use wildcard * prefix for nested class disambiguation)
./gradlew :compiler:java-direct:test --tests "*JavaUsingAstBoxTestGenerated.*testSpecificName*" -q --rerun
./gradlew :compiler:java-direct:test --tests "*JavaUsingAstPhasedTestGenerated.*testSpecificName*" -q --rerun

# PSI regression (after shared FIR file or test data changes)
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" -q
```

### Extracting failures from output

**ALWAYS use Gradle text output — never XML files.** Box test XMLs are overwritten by phased test XMLs; both share the same results directory and the split is invisible in XML.

```bash
# Get all failing test names from the saved output
grep "FAILED" /tmp/jd_test.txt | sort -u

# Get only box test failures
grep "BoxJvm.*FAILED" /tmp/jd_test.txt

# Get failures with embedded exceptions (MultipleFailuresError)
grep -A3 "FAILED" /tmp/jd_test.txt | grep -E "IllegalState|NoSuch|Exception|Error:" | head -20
```

### Verifying PSI behavior for a failing test

**Do NOT try to run `FirLightTreeBlackBoxCodegenTestGenerated.*testName*`** — the `$` in nested class names causes the Gradle filter to silently match nothing (BUILD SUCCESSFUL with 0 tests). It wastes time with no signal.

Instead, for box tests use `--info` and check the exception:
```bash
./gradlew :compiler:java-direct:test --tests "*BoxTestGenerated.*testFoo*" --rerun --info 2>&1 | grep -E "FAILED|Exception|Error:" | head -10
```

For shared file changes, compare with upstream **before** tracing FIR internals:
```bash
git show origin/master:compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt | grep -A8 "relevantSection"
```

---

## What NOT to Do

- **Don't modify test data** to make java-direct tests pass — fix the implementation instead
- **Don't hardcode lists** for resolution/filtering — use callback pattern
- **Don't modify shared FIR files or test data** without running PSI regression tests
- **Don't assume AST token names** — always dump and verify via exception debugging
- **Don't estimate fix counts without debugging** — verify root cause with 2-3 tests first
- **Don't implement partial interfaces** — e.g., `JavaAnnotationArgument` requires value subinterfaces, not just `name`
- **Don't create git commits** — user reviews all changes first
- **Don't persist a failing approach** — if a fix causes net regressions, revert it and document the limitation
- **Don't parse XML test result files** — they are stale and unreliable; use Gradle text output
- **Don't run `FirLightTreeBlackBoxCodegenTestGenerated.*testName*` to verify PSI behavior** — the nested `$` class filter silently matches nothing and wastes time; use `git show origin/master:...` instead
- **Don't trace deep FIR2IR internals before checking upstream** — for any shared file, compare `git show origin/master:` first; fixes are often already there
- **Don't fix one implicit-Java-rule corner case at a time** — when a fix involves implicit Java modifiers (visibility, static, final, abstract), audit the ENTIRE file against the reference and fix all discrepancies at once

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

*Last updated: 2026-03-16 (iter 36 retrospective)*
