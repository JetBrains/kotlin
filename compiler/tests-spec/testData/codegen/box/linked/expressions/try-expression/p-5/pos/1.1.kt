// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, try-expression -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: expressions, try-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If no exception is thrown during the evaluation of the try body, no catch blocks are executed, the finally block is evaluated after the try body, and the program execution continues as normal.
 */


fun box(): String {
    var isTryExecuted = false
    var isCatched = false
    var isFinallyExecuted = false
    try {
        isTryExecuted = true
    } catch (e: Exception) {
        isCatched = true
    } finally {
        isFinallyExecuted = true
    }
    return if (isTryExecuted && !isCatched && isFinallyExecuted) "OK"
    else "NOK"
}