/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression -> paragraph 5 -> sentence 1
 */
fun foo(x: Int, y: Int): Int =
        when {
            x > 0, y > 0,<!SYNTAX!>,<!> x < 0 -> 1
            else -> 0
        }

fun bar(x: Int): Int =
        when (x) {
            0 -> 0
            else -> 1
        }
