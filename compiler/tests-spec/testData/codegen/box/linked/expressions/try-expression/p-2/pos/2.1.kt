// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 2 -> sentence 2
 * PRIMARY LINKS: expressions, try-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: catch block is evaluated immediately after the exception is thrown and the exception itself is passed inside the catch block as the corresponding parameter.
 */

fun throwException(b: Boolean) = run { if (b) throw Exception() }


fun box(): String {
    var flag = false
    try {
        throwException(true)
        flag = true
    } catch (e: Exception) {
        if (!flag)
            return "OK"
    }
    return "NOK"
}