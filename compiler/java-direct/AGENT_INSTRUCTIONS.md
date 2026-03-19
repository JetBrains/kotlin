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
- **NEVER run `-Pkotlin.test.update.test.data=true`** — this modifies shared test data in TWO directories (`compiler/testData/` AND `compiler/fir/analysis-tests/testData/`) and will corrupt PSI test expectations. Use `--info` and grep for assertion messages instead.

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

### Revert-First Policy
- **If a fix introduces ANY regression** (new test failures that weren't failing before), **revert immediately**. Do not try to patch the regression on top of the fix — this leads to cascading complexity.
- After reverting, document what went wrong and why, then take a different approach.
- A net improvement of +3 fixed / -2 regressed is NOT acceptable. Aim for zero regressions.

---

## Resolution Pipeline (MUST READ before resolution fixes)

**Read `implDocs/RESOLUTION_PIPELINE.md`** before making any changes to type resolution, import resolution, annotation resolution, or any code in `JavaResolutionContext.kt`, `JavaTypeConversion.kt`, or `javaTypes.kt`.

Key principle: **When string-based resolution is ambiguous (same string maps to multiple ClassIds), return `ClassId` directly to preserve the package/class boundary.** This was learned the hard way in iteration 43.

---

## Mandatory Triage Before Each Iteration

**Before diving into a fix, categorize ALL remaining failures:**

1. Run both test suites ONCE, save output to `/tmp/jdb_test.txt` and `/tmp/jdp_test.txt`
2. Extract ALL failing test names: `grep "FAILED" /tmp/jdb_test.txt /tmp/jdp_test.txt | sort -u`
3. For each failure, extract the FIRST exception/assertion line:
   ```bash
   grep -A5 "FAILED" /tmp/jdp_test.txt | grep -E "IllegalState|NoSuch|Exception|Error:|UNRESOLVED|MISSING|Actual data" | head -60
   ```
4. Group failures by error pattern (not test name) — same error pattern often shares a root cause
5. Pick the LARGEST group and debug 2-3 tests from it to confirm shared root cause
6. Only then start implementing a fix

**Why**: Without triage, you'll pick a random 1-test failure and spend an iteration on it while a 10-test cluster waits. Triage maximizes tests-fixed-per-iteration.

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

**BEFORE modifying shared files**, run PSI regression tests to establish a baseline:
```bash
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" -q
```

**AFTER modifying shared files**, run PSI regression tests again and compare. If ANY new PSI failures appear, **revert immediately** — do not try to fix the PSI regression on top.

**Pre-flight checklist for shared file changes:**
- [ ] PSI regression baseline captured (BEFORE making changes)
- [ ] `git show origin/master:<file>` compared to see upstream state
- [ ] Change is minimal and targeted — no speculative refactoring in shared files
- [ ] PSI regression re-run AFTER changes shows zero new failures

---

## Test Commands

```bash
# Box tests (~1168 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" --rerun-tasks --no-build-cache 2>&1 | tee /tmp/jdb_test.txt

# Phased/diagnostic tests (~1442 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --rerun-tasks --no-build-cache 2>&1 | tee /tmp/jdp_test.txt

# Both suites together (2610 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated" --rerun-tasks --no-build-cache 2>&1 | tee /tmp/jd_test.txt

# Unit tests (40 tests - MUST stay green)
./gradlew :compiler:java-direct:test --tests "JavaParsingTest" -q

# Single test (use wildcard * prefix for nested class disambiguation)
./gradlew :compiler:java-direct:test --tests "*JavaUsingAstBoxTestGenerated.*testSpecificName*" -q --rerun
./gradlew :compiler:java-direct:test --tests "*JavaUsingAstPhasedTestGenerated.*testSpecificName*" -q --rerun

# PSI regression (after shared FIR file or test data changes)
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" -q
```

### Test Result Caching — Run Once, Grep Many Times

**Run the full suite ONCE after each code change, then grep `/tmp/jd_test.txt` for every filter you need. NEVER rerun the same suite just to see a different slice of the results.**

If the relevant code has not changed since the last run, the output cannot have changed either — use the saved file directly.

**PSI regression tests are independent of java-direct-specific changes.** If only files under `compiler/java-direct/src/**` were modified (no shared FIR files, no test data), skip the PSI regression run — its results cannot have changed.

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
./gradlew :compiler:java-direct:test --tests "*BoxTestGenerated.*testFoo*" --rerun-tasks --info 2>&1 | grep -E "FAILED|Exception|Error:" | head -10
```

For shared file changes, compare with upstream **before** tracing FIR internals:
```bash
git show origin/master:compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt | grep -A8 "relevantSection"
```

---

## Debugging Recipes

### Quick: See what java-direct produces vs what PSI produces
Add a temporary exception throw in the java-direct code path:
```kotlin
// In the relevant OverAst class:
throw IllegalStateException("DEBUG: propertyName=$value, classifierQualifiedName=$classifierQualifiedName")
```
Then run a SINGLE test: `./gradlew :compiler:java-direct:test --tests "*testName*" -q --rerun`

The exception message appears in the test failure output. This is the **only** reliable way to see runtime values — `println()` is swallowed by Gradle.

### Quick: See the AST structure for a Java construct
```kotlin
throw IllegalStateException("DEBUG AST:\n${node.dump()}")
// where dump() is defined in INVESTIGATION_TECHNIQUES.md
```

### Quick: Understand why a type resolves to the wrong class
Add debug in `JavaResolutionContext.resolve()`:
```kotlin
if (name == "a.b") { // match the specific type you're investigating
    throw IllegalStateException("DEBUG resolve: name=$name, result=$result")
}
```

### File-based logging (when exception debugging is too disruptive)
```kotlin
java.io.File("/tmp/java_direct_debug.log").appendText("DEBUG: $message\n")
```
Useful when you need to trace multiple calls without crashing the test.

---

## What NOT to Do

- **Don't modify test data** to make java-direct tests pass — fix the implementation instead
- **Don't run `-Pkotlin.test.update.test.data=true`** — it silently corrupts shared test data in multiple directories
- **Don't hardcode lists** for resolution/filtering — use callback pattern
- **Don't modify shared FIR files or test data** without running PSI regression BEFORE and AFTER
- **Don't assume AST token names** — always dump and verify via exception debugging
- **Don't estimate fix counts without debugging** — verify root cause with 2-3 tests first
- **Don't implement partial interfaces** — e.g., `JavaAnnotationArgument` requires value subinterfaces, not just `name`
- **Don't create git commits** — user reviews all changes first
- **Don't persist a failing approach** — if a fix causes ANY net regressions, revert immediately and document
- **Don't parse XML test result files** — they are stale and unreliable; use Gradle text output
- **Don't rerun Gradle to get a different view of results** — run once, save with `tee /tmp/jd_test.txt` (use distinkt file name for each test suite), then grep the file for any slice you need
- **Don't run PSI regression tests after java-direct-only changes** — if no shared FIR files or test data were modified, PSI results cannot change; skip the run
- **Don't run `FirLightTreeBlackBoxCodegenTestGenerated.*testName*` to verify PSI behavior** — the nested `$` class filter silently matches nothing and wastes time; use `git show origin/master:...` instead
- **Don't trace deep FIR2IR internals before checking upstream** — for any shared file, compare `git show origin/master:` first; fixes are often already there
- **Don't fix one implicit-Java-rule corner case at a time** — when a fix involves implicit Java modifiers (visibility, static, final, abstract), audit the ENTIRE file against the reference and fix all discrepancies at once
- **Don't change `findClassId` probe order in `JavaTypeConversion.kt`** — it's shared with PSI. Fix resolution priority in `JavaResolutionContext.resolve()` instead
- **Don't return ambiguous strings from resolution** — when a qualified name like `"a.b"` could mean either `ClassId("a","b")` or `ClassId("","a.b")`, use `ClassId`-based resolution that encodes the boundary. See `implDocs/RESOLUTION_PIPELINE.md`
- **Don't handwave "no net change" as a test infrastructure issue** — if your fix is correct but the test count doesn't change, there's a bug. Either the fix doesn't reach the failing tests, or it fixes some and breaks others. Investigate.

---

## Iteration Process

Each iteration should be **short and focused** — target 1 specific root cause, not a vague "area":

1. **Triage** — categorize all failures, pick the largest group (see Mandatory Triage above)
2. **Debug** — confirm root cause with 2-3 representative tests using exception-based debugging
3. **Check reference** — read the PSI/javac-wrapper equivalent before writing any fix
4. **Implement** — minimal, targeted fix for the confirmed root cause
5. **Verify** — run full suite. If ANY regressions: **revert immediately**, document, take a different approach
6. **Document** — update ITERATION_RESULTS.md with root cause, fix, and exact test counts

**If the fix doesn't change test counts**: do NOT move on. Investigate WHY — the fix either doesn't reach the failing code path, or it fixes some tests while breaking others. Both cases need understanding.

**If you're stuck after 30 minutes on a single issue**: stop, document what you've tried and what you've learned, and ask the user for guidance. Don't keep trying variations of the same approach.

---

## Detailed Reference Documents

| Document | When to consult |
|----------|----------------|
| `implDocs/RESOLUTION_PIPELINE.md` | **MUST READ** before any resolution fix — type/annotation/import resolution call chain |
| `implDocs/ARCHITECTURE.md` | Callback patterns, key files, reference implementations, common fixes |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Debugging techniques, categorization script, AST inspection |
| `FIXING_ITERATIONS.md` | Current state, remaining categories, next iteration plans |
| `ITERATION_RESULTS.md` | History of what was tried and learned |
| `IMPLEMENTATION_PLAN.md` | High-level architecture overview |
| `implDocs/archive/` | Archived iteration details (deep context recovery only) |

---

*Last updated: 2026-03-19 (post iter-43 retrospective)*
