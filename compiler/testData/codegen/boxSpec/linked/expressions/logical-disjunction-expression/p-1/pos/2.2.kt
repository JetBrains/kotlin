// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, logical-disjunction-expression -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, logical-disjunction-expression -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: operator || does not evaluate the right hand side argument unless the left hand side argument evaluated to false
 */

fun box(): String {
    val x: Boolean
    try {
        x = false || false || true || throw MyException()
    } catch (e: MyException) {
        return "NOK"
    }
    if (x)
        return "OK"
    return "NOK"
}

class MyException : Exception() {}


