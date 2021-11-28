// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 5 -> sentence 2
 * PRIMARY LINKS: expressions, try-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: the finally block is evaluated after the evaluation of the matching catch block.
 */

fun throwException(b: Boolean) = run { if (b) throw Exception() }

fun box(): String {
    var isTryExecuted = false
    var isCatched = false
    var isFinallyExecuted = false
    try {
        isTryExecuted = true
        throwException(true)
    } catch (e: Exception) {
        isCatched = true
    } finally {
        isFinallyExecuted = true
    }
    return if (isTryExecuted &&isCatched && isFinallyExecuted) "OK"
    else "NOK"
}