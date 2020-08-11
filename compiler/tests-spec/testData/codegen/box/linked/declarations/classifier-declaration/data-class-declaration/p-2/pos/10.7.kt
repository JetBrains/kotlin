// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 10
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 7
 * NUMBER: 7
 * DESCRIPTION: copy with defaults
 */

open class B(open val a: Int, open val b: Any) {
    override fun hashCode(): Int {
        return 555
    }
}

class C(val x: Int)

data class A(override val a: Int = 1, override val b: C = C(1)) : B(a, b)

fun box(): String {
    val x = A(10)
    val y = x.copy()
    if (y.a == x.a && y.b == x.b) {
        return "OK"
    } else return "nok"
}

