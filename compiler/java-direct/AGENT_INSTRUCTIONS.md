# Java-Direct: Agent Instructions

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Key files**: `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`, `JavaMemberOverAst.kt`, `JavaResolutionContext.kt`, `JavaClassFinderOverAstImpl.kt`

---

## ⚠ Non-Negotiable Rules (stop immediately if violated)

1. **No command chaining** — NEVER use `&&`, `||`, or `;`. Each command = one tool call.
   Why: the permission system only checks the first token. 

2. **Always pipe Gradle output to `tee "$JD_TMP/..."`** — no exceptions.
   If you forgot `tee`: do NOT rerun Gradle. Grep whatever output you have, or ask the user.

3. **Only the main agent runs Gradle** — subagents MUST NOT invoke `./gradlew`.
   Why: parallel builds corrupt each other's test results and cause excessive CPU and disk load.

---

## Shell Discipline

### Session Working Directory

At the start of each session, create a unique temp directory and use it for ALL temp files:
```bash
export JD_TMP="/tmp/jd_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$JD_TMP"
```
All temp file paths in this document use `$JD_TMP`. **NEVER write directly to `/tmp/`** — always use the session directory.

### One Command Per Execution

**NEVER chain commands with `&&`, `||`, or `;`.** Each command must be a separate tool call. The only exception is piping (`|`), which is a single logical operation.

The permission system matches on the **first token only**. With `cmd1 && cmd2`, only `cmd1` is checked — `cmd2` runs without the user seeing it, bypassing review entirely.

- **Wrong**: `rm -f "$JD_TMP/debug.log" && ./gradlew ...`
- **Wrong**: `cp file.kt /tmp/file.kt.orig && echo "done"`
- **Correct**: Run `rm -f "$JD_TMP/debug.log"` first, then `./gradlew ...` as a separate call

### Gradle Runs: Save Output, Run Once

**Every Gradle invocation MUST save output** with `tee` to a file in `$JD_TMP`. Always include `--stacktrace` for full suite and single-test runs. Do NOT use `--info` or `--debug` unless specifically needed — they produce too much noise.

After running a suite, **grep the saved file** — never rerun Gradle just to see a different slice. If code hasn't changed, the output hasn't changed.

**Only the main agent runs Gradle.** Subagents may only analyze saved log files — they must NEVER invoke Gradle themselves. Parallel builds corrupt each other's test results and cause excessive CPU and disk load.

### File-based Debug Logging

When using file-based logging, write to the session directory:
```kotlin
java.io.File("$JD_TMP/debug.log").appendText("DEBUG: $message\n")
```
Replace `$JD_TMP` with the actual path value at runtime.

---

## Ground Rules

- **Use JetBrains MCP tools** for all project file operations (see `.ai/guidelines.md`)
- **NEVER create git commits** — all changes must be reviewed by the user first
- **NEVER run `-Pkotlin.test.update.test.data=true`** — it corrupts shared test data in `compiler/testData/` AND `compiler/fir/analysis-tests/testData/`
- **NEVER modify test data** to make java-direct tests pass — fix the implementation
- **FIR terminology**: `simpleImports`/`starImports` (NOT singleType/onDemand)
- **Update ITERATION_RESULTS.md** after each iteration
- **Check files with `get_file_problems`** after changes
- **Check `git diff` for unintended changes** after every test run

### Test Data Is Ground Truth

Test data files (`compiler/testData/`) are shared between java-direct and PSI test runners. If java-direct produces a different result, the java-direct implementation is wrong — fix it. Or document it as a known acceptable difference in ITERATION_RESULTS.md.

### Revert-First Policy

If a fix introduces ANY regression, **revert immediately**. Do not patch on top. A net improvement of +3/-2 is NOT acceptable — aim for zero regressions.

---

## Test Commands

```bash
# Both suites together (~2610 tests) — preferred for verification
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jd_test.txt"

# Box tests only (~1168 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jdb_test.txt"

# Phased/diagnostic tests only (~1442 tests)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --stacktrace --rerun-tasks --no-build-cache 2>&1 | tee "$JD_TMP/jdp_test.txt"

# Unit tests (MUST stay green)
./gradlew :compiler:java-direct:test --tests "JavaParsingTest" --stacktrace -q

# Single test
./gradlew :compiler:java-direct:test --tests "*JavaUsingAstBoxTestGenerated.*testSpecificName*" --stacktrace -q --rerun 2>&1 | tee "$JD_TMP/single_test.txt"

# PSI regression (only after shared FIR file or test data changes)
./gradlew :compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" --stacktrace -q 2>&1 | tee "$JD_TMP/psi_test.txt"
```

### Extracting Failures

**Use saved Gradle text output — never XML files** (box/phased share the same results directory).

```bash
grep "FAILED" "$JD_TMP/jd_test.txt" | sort -u
grep "BoxJvm.*FAILED" "$JD_TMP/jd_test.txt"
grep -A5 "FAILED" "$JD_TMP/jd_test.txt" | grep -E "IllegalState|NoSuch|Exception|Error:|UNRESOLVED|MISSING|Actual data" | head -60
```

### PSI Regression

Skip PSI regression if only `compiler/java-direct/src/**` was modified. Run BEFORE and AFTER shared file changes. If new failures appear, **revert immediately**.

**Shared FIR files** (modify with caution): `FirJavaFacade.kt`, `JavaTypeConversion.kt`, `javaAnnotationsMapping.kt`, `core/compiler.common.jvm/.../load/java/structure/*.kt`

For shared files, always compare with upstream first: `git show origin/master:<path> | grep -A10 "relevantFunction"`

---

## Triage (MANDATORY Before Each Iteration)

1. Run both suites ONCE, save output
2. Extract ALL failing test names from saved output
3. Group by error pattern (not test name) — same pattern often shares root cause
4. Pick the LARGEST group, debug 2-3 tests to confirm shared root cause
5. Only then implement a fix

---

## Fixing Approach

### Preferred: Reference-First Area Audit

1. Pick a code area (e.g., member visibility, type parameters, annotations)
2. Read the reference: javac-wrapper (`TreeBasedClass.kt`, `TreeBasedField.kt`, `TreeBasedMethod.kt`) or PSI (`JavaClassImpl.java`, `JavaMemberImpl.java`)
3. Compare property by property with java-direct (`JavaClassOverAst.kt`, `JavaMemberOverAst.kt`, etc.)
4. List ALL discrepancies in one pass, fix all together

### Alternative: Ad-hoc (for isolated exceptions)

1. Pick exception from saved test output — non-AssertionError first
2. Debug 2-3 representative tests
3. Check reference implementation
4. Implement targeted fix

### Pre-Implementation Checklist

- [ ] Checked reference implementation (javac-wrapper or PSI)
- [ ] For shared files: compared with `git show origin/master:...`
- [ ] Verified AST node names (token names differ from constant names — check `SyntaxElementType("...")` in JavaSyntaxTokenType.kt)
- [ ] Confirmed root cause by debugging 2-3 tests

---

## Debugging Recipes

**Exception-based** (preferred — output appears in test failure):
```kotlin
throw IllegalStateException("DEBUG: propertyName=$value")
```

**AST dump**: `throw IllegalStateException("DEBUG AST:\n${node.dump()}")` (see `implDocs/INVESTIGATION_TECHNIQUES.md`)

**File-based logging** (when exception debugging is too disruptive):
```kotlin
java.io.File("<JD_TMP>/debug.log").appendText("DEBUG: $message\n")
```

`println()` is swallowed by Gradle — never use it for debugging.

---

## Iteration Process

Target 1 specific root cause per iteration:

1. **Triage** — categorize failures, pick largest group
2. **Debug** — confirm root cause with 2-3 tests
3. **Check reference** — read PSI/javac-wrapper equivalent
4. **Implement** — minimal fix
5. **Verify** — run full suite (saved to `$JD_TMP`). ANY regressions → revert
6. **Document** — update ITERATION_RESULTS.md

**If test counts don't change**: investigate — the fix either misses the code path or fixes some while breaking others.

**If stuck 30 minutes on one issue**: stop, document, ask user.

---

## What NOT to Do

- Don't rerun Gradle for a different view of results — grep the saved log file
- Don't chain shell commands with `&&` — one command per tool call (permission system only checks first token)
- Don't let subagents run Gradle — parallel builds corrupt results and overload CPU/disk
- Don't use `--info`/`--debug` unless specifically necessary
- Don't hardcode lists for resolution — use callback pattern
- Don't assume AST token names — always verify
- Don't estimate fix counts without debugging 2-3 tests first
- Don't change `findClassId` probe order in `JavaTypeConversion.kt` (shared with PSI) — fix in `JavaResolutionContext.resolve()`
- Don't return ambiguous strings from resolution — use `ClassId`-based resolution (see `implDocs/RESOLUTION_PIPELINE.md`)
- Don't run `FirLightTreeBlackBoxCodegenTestGenerated.*testName*` — nested `$` silently matches nothing
- Don't fix implicit-Java-rule corner cases one at a time — audit the entire file against reference

---

## Resolution Pipeline

**Read `implDocs/RESOLUTION_PIPELINE.md`** before any changes to type/annotation/import resolution or code in `JavaResolutionContext.kt`, `JavaTypeConversion.kt`, `javaTypes.kt`.

Key principle: when string-based resolution is ambiguous, return `ClassId` directly to preserve the package/class boundary.

---

## Reference Documents

| Document | When to consult |
|----------|----------------|
| `implDocs/RESOLUTION_PIPELINE.md` | Before any resolution fix |
| `implDocs/ARCHITECTURE.md` | Callback patterns, key files, reference implementations |
| `implDocs/INVESTIGATION_TECHNIQUES.md` | Debugging techniques, AST inspection |
| `FIXING_ITERATIONS.md` | Current state, remaining categories |
| `ITERATION_RESULTS.md` | History of iterations |
| `implDocs/archive/` | Archived iteration details |

---

*Last updated: 2026-03-24 (non-negotiable rules block, permission system explanation, subagent reasons)*
