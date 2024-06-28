// FIR_IDENTICAL
// LANGUAGE: +ProhibitSimplificationOfNonTrivialConstBooleanExpressions
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 4
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 5
 */

// See also: KT-3743
fun foo(arg: Boolean): String {
    // Must be exhaustive
    return <!NO_ELSE_IN_WHEN!>when<!>(arg) {
        (2 == 2) -> "truth"
        (2 == 1) -> "falsehood"
    }
}
