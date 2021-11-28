/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 5
 */

// KT-4434 Missed diagnostic about else branch in when

package test

fun foo(): Int {
    val a = "a"
    return <!RETURN_TYPE_MISMATCH!>if (a.length > 0) {
        when (a) {
            "a" -> 1
        }
    }
    else {
        3
    }<!>
}

fun bar(): Int {
    val a = "a"
    if (a.length > 0) {
        return <!NO_ELSE_IN_WHEN!>when<!> (a) {
            "a" -> 1
        }
    }
    else {
        return 3
    }
}
