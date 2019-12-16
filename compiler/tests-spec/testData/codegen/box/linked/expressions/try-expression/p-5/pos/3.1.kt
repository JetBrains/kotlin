// SKIP_TXT

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, try-expression -> paragraph 5 -> sentence 3
 * RELEVANT PLACES: expressions, try-expression -> paragraph 4 -> sentence 1
 * exceptions, catching-exceptions -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If an exception was thrown, but no catch block matched its type, the finally block is evaluated before propagating the exception up the call stack
 */
fun throwExceptionA(b: Boolean) = run { if (b) throw ExcA() }

class ExcA() : Exception()
class ExcB() : Exception()

fun box(): String {
    var isTryExecuted = false
    var isCatched = false
    var isFinallyExecuted = false
    var isTryExpressionPropagated = false

    try {
        try {
            isTryExecuted = true
            throwExceptionA(true)
        } catch (e: ExcB) {
            isCatched = true
        } finally {
            isFinallyExecuted = true
        }
    } catch (e: ExcA) {
        isTryExpressionPropagated = true
    }

    return if (isTryExecuted &&
        !isCatched &&
        isFinallyExecuted &&
        isTryExpressionPropagated
    ) "OK"
    else "NOK"
}