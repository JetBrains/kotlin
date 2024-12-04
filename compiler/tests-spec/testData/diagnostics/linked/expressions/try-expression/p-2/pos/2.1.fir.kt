// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 2 -> sentence 2
 * PRIMARY LINKS: expressions, try-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: catch block is evaluated immediately after the exception is thrown and the exception itself is passed inside the catch block as the corresponding parameter.
 */


// TESTCASE NUMBER: 1

fun case1() {
    var flag = false
    try {
        throw Exception()
        flag = true
    } catch (e: Exception) {
        assert(!flag)
    }
}
