# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-04-17 (refactoring step 1.3)

---

## Refactoring Step 1.3: Extract Duplicate Literal Parsing - 2026-04-17

### Root Cause Analysis
`JavaAnnotationOverAst.kt` (lines ~230–336) and `ConstantEvaluator.kt` (companion, lines ~293–400) each carried private copies of `parseIntegerLiteral`, `parseLongLiteral`, `parseFloatLiteral`, `parseDoubleLiteral`, and `unescapeJavaString`. The two integer-parser copies also diverged slightly: `ConstantEvaluator` included an extra `cleaned.all { it in '0'..'7' }` guard on the octal branch, avoiding a misclassification of decimal numbers starting with `0` (e.g. `09`) as octal.

### Fix
Created `JavaLiteralParser.kt` — an `internal object` consolidating all five helpers, keeping the safer octal guard from `ConstantEvaluator`. Updated both call sites:

- `JavaAnnotationOverAst.kt`: removed the top-level private helpers and the `String.unescapeJavaString` extension; call sites in `evaluateLiteral` now delegate to `JavaLiteralParser.parseIntegerLiteral` / `parseLongLiteral` / `parseFloatLiteral` / `parseDoubleLiteral` / `unescapeJavaString(...)`.
- `ConstantEvaluator.kt`: removed the `companion object` body (was only hosting the duplicated helpers); `evaluateLiteral` delegates to `JavaLiteralParser` the same way.

No behavioral change beyond the minor integer-octal unification, which matches `ConstantEvaluator`'s pre-existing (more correct) behavior.

Files modified: `JavaLiteralParser.kt` (new), `JavaAnnotationOverAst.kt`, `ConstantEvaluator.kt`.

### Test Results
- Combined suite `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL**, 0 `FAILED` lines in the log.
- Baseline preserved: 1168/1168 box, 1454/1456 phased (2 known won't-fix).

### Key Learnings
- When consolidating "duplicate" helpers, diff the two versions carefully — minor guards can be load-bearing (octal detection here).
- Keeping the shared utility as an `internal object` (not extension functions) avoids polluting `String` with Java-literal-specific semantics, which would otherwise leak into unrelated call sites.

---

## Archives

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1–6 | 0 → 90/138 box (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7–16 | 90 → 1075/1166 box (92.2%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17–23 | 1075 → 1150/1167 box, 1374/1442 phased (95.3%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24–26 | 1150/1167 → same, phased 300 → 1374/1442 |
| `implDocs/archive/ITERATIONS_27_36_DETAILS.md` | 27–36 | 1150/1167 → 1157/1168 box, **79 combined failing** |
| `implDocs/archive/ITERATIONS_37_51_DETAILS.md` | 37–51 | 1157/1168 → 1165/1168 box, **17 combined failing** |
| `implDocs/archive/ITERATIONS_52_71_DETAILS.md` | 52–71 | 1165/1168 → 1168/1168 box, 1454/1456 phased, **2 won't-fix**; perf + refactoring |

---

## Future Iteration Template

~~~markdown
## Iteration N: [Title] - YYYY-MM-DD

### Root Cause Analysis
[Reference-first: check javac-wrapper / PSI / git show origin/master first]

### Fix
[Files modified, solution description]

### Test Results
- Box: X/1168, Phased: X/1443, Total failing: N

### Key Learnings
~~~
