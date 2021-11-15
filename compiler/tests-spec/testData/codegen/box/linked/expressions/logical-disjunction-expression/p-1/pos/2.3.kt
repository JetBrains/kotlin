// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, logical-disjunction-expression -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, logical-disjunction-expression -> paragraph 1 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: operator || does not evaluate the right hand side argument unless the left hand side argument evaluated to false
 */

fun box(): String {
    val x: Boolean = false
    try {
        x = (throw MyException()) || true
    } catch (e: MyException) {
        if (!x)
            return "OK"
    }
    return "NOK"
}

class MyException : Exception() {}