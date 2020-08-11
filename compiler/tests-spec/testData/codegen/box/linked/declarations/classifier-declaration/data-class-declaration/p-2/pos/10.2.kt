// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 10
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 7
 * NUMBER: 2
 * DESCRIPTION: copy with defaults
 */

open class B(open val a: Int, open val b: Any)
data class A(override val a: Int, override val b: C) : B(a, b)
class C

fun box(): String {
    val x = A(1, C())
    val y =x.copy()

    if (y.a == x.a && y.b == x.b) {
        return "OK"
    } else return "nok"
}