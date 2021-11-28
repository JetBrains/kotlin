// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 8
 * DESCRIPTION: check kotlin.Nothing type
 */

fun box(): String {
    val c = Class()
    try {
        val a: () -> Nothing = c.data
        a.invoke()
    } catch (e: IllegalArgumentException) {
        try {
            val d: () -> Nothing = c.value
            d()
        } catch (e: NotImplementedError) {
            return "OK"
        }

    }
    return "NOK"
}

open class Class() {
    var data: () -> Nothing = { throwException("data") as Nothing }
    val value: () -> Nothing
        get() = { TODO() as Nothing }
}

private fun throwException(m: String): Any {
    throw  IllegalArgumentException(m)
}
