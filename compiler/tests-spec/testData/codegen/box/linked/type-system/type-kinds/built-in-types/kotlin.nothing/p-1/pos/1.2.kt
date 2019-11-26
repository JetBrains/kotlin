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
    return s ?: throw IllegalArgumentException("not null string is expected");
}

fun box(): String {
    val b = foo("")
    try {
        val a = foo(null)
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
    return "NOK"
}