// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: todo
 */

fun foo(): Nothing {
    throw Exception()
}

fun box() {
    try {
        foo()
    } catch (e: Exception) {
        return "OK"
    }
    return "NOK"
}
