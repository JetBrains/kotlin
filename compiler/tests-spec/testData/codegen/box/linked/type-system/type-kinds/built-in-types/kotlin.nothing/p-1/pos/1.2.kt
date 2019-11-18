// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: type-system, type-kinds, built-in-types, kotlin.nothing -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: todo
 */


fun foo(s: String?): String {
    val data = s ?: throw IllegalArgumentException("not null string is expected");
}

fun box() {
    val result = "NOK"
    val b = foo("")
    try {
        val a = foo(null)
    } catch (e: IllegalArgumentException) {
        result = "OK"
    }
    return result
}