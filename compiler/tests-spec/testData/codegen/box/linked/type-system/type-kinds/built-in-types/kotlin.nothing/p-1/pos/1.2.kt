// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: type-system, type-kinds, built-in-types, kotlin.nothing -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: check the allowance of returning Nothing where String is expected
 */


fun foo(s: String?): String {
    return s ?: throw IllegalArgumentException("not null string is expected");
}

fun box(): String {
    var result = "NOK"
    val b = foo("")
    try {
        val a = foo(null)
    } catch (e: IllegalArgumentException) {
        result = "OK"
    }
    return result
}