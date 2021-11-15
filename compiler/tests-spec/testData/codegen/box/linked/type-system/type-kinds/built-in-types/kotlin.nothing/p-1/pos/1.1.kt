// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: type-system, type-kinds, built-in-types, kotlin.nothing -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: todo
 */

fun box(): String {
    val b : Nothing?= null
    try {
        val a: Int = b!!
    } catch (e: NullPointerException) {
        return "OK"
    }
    return "NOK"
}