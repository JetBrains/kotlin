// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, logical-conjunction-expression -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, logical-conjunction-expression -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: operator && does not evaluate the right hand side argument unless the left hand side argument evaluated to false.
 */

fun box(): String {
    val x: Boolean
    try {
        x = true && true && false && throw MyException()
    } catch (e: MyException) {
        return "NOK"
    }
    if (x)
        return "NOK"
    return "OK"
}

class MyException : Exception() {}