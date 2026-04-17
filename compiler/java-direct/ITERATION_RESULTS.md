# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-04-17 (refactoring step 1.4)

---

## Refactoring Step 1.4: ConstantEvaluator vs FirExpressionEvaluator — Investigation - 2026-04-17

### Question (from REFACTORING_PLAN.md)
Can the java-direct `ConstantEvaluator` (~290 LOC after Step 1.3) be replaced by FIR's `FirExpressionEvaluator` so Java constant folding reuses the canonical FIR evaluator?

### Investigation — What Each Evaluator Operates On

| Evaluator | Input | Output | Stage |
|-----------|-------|--------|-------|
| `ConstantEvaluator` (java-direct, `ConstantEvaluator.kt`) | `JavaSyntaxNode` — raw Java KMP-parser AST (`LITERAL_EXPRESSION`, `BINARY_EXPRESSION`, `REFERENCE_EXPRESSION`, …) | `Any?` — a Kotlin primitive/String/null | During Java model construction, before any FIR is built |
| `FirExpressionEvaluator.evaluateExpression(expr, session)` (`compiler/fir/providers/src/.../FirExpressionEvaluator.kt`, 704 LOC) | `FirExpression` — already fully-built & resolved FIR tree (`FirLiteralExpression`, `FirFunctionCall`, `FirPropertyAccessExpression`, …) | `FirEvaluatorResult` (wrapping a `FirLiteralExpression` or diagnostic) | During FIR resolution, after symbol & type resolution |

### Call Chain

1. **Consumer of `ConstantEvaluator`** — sole caller is `JavaFieldOverAst.initializerValue` / `resolveInitializerValue` (`JavaMemberOverAst.kt:244–255`).
2. **Who calls those?** — `FirJavaFacade.kt:567–576` (`lazyInitializer = lazy { ... }` of `buildJavaField`):
   ```kotlin
   lazyInitializer = lazy {
       javaField.initializerValue?.createConstantIfAny(session)
           ?: javaField.resolveInitializerValue { classQualifier, fieldName ->
               resolveExternalFieldValue(session, classQualifier, fieldName, classId.packageFqName)
           }?.createConstantIfAny(session)
   }
   ```
   This runs during FIR Java symbol provider materialization — we are *producing* the `FirField`'s initializer and need a plain `Any?` right now. There is no pre-existing `FirExpression` for the Java initializer: the Java-direct module never converts Java expressions to FIR.
3. **Interaction with `FirExpressionEvaluator`**: `FirJavaFacade.kt` line 32 already imports `FirExpressionEvaluator`, and `resolveExternalFieldValue` uses it (indirectly, via `extractConstantValue` on a Kotlin `FirPropertySymbol`) to resolve *the Kotlin side* of the cross-language callback — e.g. `MainKt.FOO` where `FOO` is a Kotlin `const val`. So the two evaluators already coexist on opposite sides of the Java→Kotlin boundary.

### Why a Direct Swap Is Not Feasible

- `FirExpressionEvaluator` fundamentally requires `FirExpression` inputs. The java-direct pipeline has no `FirExpression` for a Java field initializer — it has a raw KMP `JavaSyntaxNode`.
- Building one would require a new **Java-AST → FIR-expression** conversion layer (equivalent to what the old PSI-based Java-to-FIR converter did for method bodies and initializers), which is a substantially larger architectural change than the goal of this refactoring plan.
- Even then, the conversion would need access to FIR symbol resolution for `REFERENCE_EXPRESSION`s, introducing a new ordering dependency: Java field FIR-building would have to wait on (or lazily trigger) FIR resolution of referenced Kotlin/Java symbols. Today that dependency is cleanly side-stepped via the `resolveReference` callback, which only descends into FIR for qualified cross-language refs.
- Scope: `ConstantEvaluator` handles literals (integer/long/float/double/string/char/bool/null), unary (`+`, `-`, `!`, `~`), binary & polyadic (`+`, `-`, `*`, `/`, `%`, `<<`, `>>`, `>>>`, `&`, `|`, `^`, `&&`, `||`, `==`, `!=`, `<`, `>`, `<=`, `>=`), parenthesized, conditional (`?:`), type casts, and simple/qualified field refs — this is exactly the JLS §15.29 "constant expression" subset. `FirExpressionEvaluator` is a **superset** of this functionality (it also handles Kotlin-specific calls, when-expressions, string templates, etc.), so no expressive power is gained.

### Conclusion

`ConstantEvaluator` cannot be replaced by `FirExpressionEvaluator` without first introducing a Java-AST → FIR-expression conversion layer inside `FirJavaFacade` (or earlier). The cost/benefit is poor:

- **Cost**: a new conversion layer (non-trivial — must cover all JLS §15.29 constant-expression forms, plus resolve Java class/field references to FIR symbols at the right resolution phase), plus the risk of reshuffling the FIR Java symbol-provider phase ordering.
- **Benefit**: removing ~290 LOC of fairly contained code, in exchange for non-trivial FIR conversion code of comparable size.

### Recommendation (final)

**Keep `ConstantEvaluator` as-is.** It is the correct architectural layer for Java-model-level constant folding (pre-FIR), it is contained, has no external consumers beyond `JavaMemberOverAst`, and now shares its literal-parsing core with `JavaAnnotationOverAst` via `JavaLiteralParser` (Step 1.3). No follow-up task is warranted unless/until a separate initiative introduces a Java-AST→FIR-expression converter for other reasons (e.g. method-body constant folding, which is out of scope here).

### Verification
Document-only step — no code changes, no test run required per the plan. Current baseline from Step 1.3 (1168/1168 box, 1454/1456 phased) remains authoritative.

### Key Learnings
- FIR's `FirExpressionEvaluator` is a post-resolution tool; any pre-FIR layer that needs constant folding cannot use it without first materializing FIR.
- Coexistence pattern is already in place: Java side uses `ConstantEvaluator`, Kotlin side (for cross-language refs) uses `FirExpressionEvaluator` via the `resolveReference` callback. This is a sensible seam and should be preserved.

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
