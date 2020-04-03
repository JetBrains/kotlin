// FIR_IDENTICAL
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-313, test type: neg):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 1 -> sentence 1
 *  - expressions, when-expression -> paragraph 1 -> sentence 2
 *  - expressions, when-expression -> paragraph 6 -> sentence 5
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