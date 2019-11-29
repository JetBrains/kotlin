// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 8
 * DESCRIPTION:
 */

fun box(): String {
    val c = Class()
    try {
        c.data.invoke()
    } catch (e: IllegalArgumentException) {
        try {
            c.exception.invoke()
        } catch (e: IllegalArgumentException) {
            try {
                c.value()
            } catch (e: NotImplementedError) {
                return "OK"
            }
        }
    }
    return "NOK"
}

open class Class() {
    var data: () -> Nothing = { throwException("boo") as Nothing }
    var exception: () -> CharSequence = { throwException("foo") as String }
    val value: () -> Any
        get() = { TODO() as Nothing }
}

private fun throwException(m: String): Any {
    throw  IllegalArgumentException(m)
}
