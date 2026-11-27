// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 2 -> sentence 2
 * PRIMARY LINKS: expressions, try-expression -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: catch block is evaluated immediately after the exception is thrown and the exception itself is passed inside the catch block as the corresponding parameter.
 */

fun throwExceptionB(b: Boolean) = run { if (b) throw ExcB() }


class ExcA() : Exception()
class ExcB() : Exception()

fun box(): String {

    var flag = false
    try {
        throwExceptionB(true)
        flag = true
    } catch (e: ExcA) {
        return "NOK"
    } catch (e: ExcB) {
        return if (flag)
            "NOK"
        else "OK"
    }
    return "NOK"
}
