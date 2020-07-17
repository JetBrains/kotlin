// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 1 -> sentence 1
 * expressions, when-expression -> paragraph 1 -> sentence 2
 * expressions, when-expression -> paragraph 6 -> sentence 5
 */

// EA-68871: empty when condition
enum class My { FIRST, SECOND }
fun foo(arg: My): Int {
    when (arg) {
        My.FIRST -> return 0
        <!SYNTAX!><!>-> return 4
        else -> return -1
    }
}