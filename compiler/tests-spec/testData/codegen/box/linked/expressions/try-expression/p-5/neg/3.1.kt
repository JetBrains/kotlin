// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 5 -> sentence 3
 * PRIMARY LINKS: expressions, try-expression -> paragraph 4 -> sentence 1
 * exceptions, catching-exceptions -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If an exception was thrown, but no catch block matched its type, the finally block is evaluated before propagating the exception up the call stack
 * EXCEPTION: runtime
 */

fun throwExceptionA(b: Boolean) = run { if (b) throw ExcA() }

class ExcA() : Exception()
class ExcB() : Exception()

fun box() {
    try {
        throwExceptionA(true)
    } catch (e: ExcB) {

    } finally {

    }
}