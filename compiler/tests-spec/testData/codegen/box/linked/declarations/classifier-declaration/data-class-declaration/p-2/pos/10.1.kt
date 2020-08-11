// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 10
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 7
 * NUMBER: 1
 * DESCRIPTION: copy with defaults
 */

data class A(val a: Int, val b: String)

fun box(): String {
    val x: A = A(1, "str")
    val y = x.copy()

    if (y.a == x.a && y.b == x.b) {
        return "OK"
    } else return "nok"
}