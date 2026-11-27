// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, cast-expression -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check of the cast operators as or as?
 */

fun box(): String {
    val c = Class()
    if (c.data !is () -> Nothing) return "NOK"
    val e1 : () -> Nothing = c.exception as () -> Nothing as? () -> Nothing ?: return "NOK"
    val v = c.value as () -> Nothing as? () -> Nothing ?: return "NOK"

    return "OK"
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
