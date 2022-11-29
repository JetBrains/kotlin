/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 9 -> sentence 1
 * expressions, conditional-expression -> paragraph 4 -> sentence 1
 * expressions, conditional-expression -> paragraph 5 -> sentence 1
 */

fun test1(): Int {
    val x: String = <!TYPE_MISMATCH!>if (true) {
        <!TYPE_MISMATCH, TYPE_MISMATCH!>when {
            true -> <!TYPE_MISMATCH!>Any()<!>
            else -> null
        }<!>
    } else ""<!>
    return x.hashCode()
}

fun test2(): Int {
    val x: String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!><!TYPE_MISMATCH!>when {
                        true -> <!TYPE_MISMATCH!>Any()<!>
                        else -> null
                    }<!> ?: return 0<!>
    return x.hashCode()
}
