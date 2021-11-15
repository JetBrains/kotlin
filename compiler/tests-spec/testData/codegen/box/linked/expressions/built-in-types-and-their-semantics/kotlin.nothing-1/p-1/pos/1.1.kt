// !LANGUAGE: +NewInference
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check kotlin.Nothing by throwing the exception
 */

fun foo(): Nothing {
    throw Exception()
}

fun box(): String {
    try {
        foo()
    } catch (e: Exception) {
        return "OK"
    }
    return "NOK"
}
