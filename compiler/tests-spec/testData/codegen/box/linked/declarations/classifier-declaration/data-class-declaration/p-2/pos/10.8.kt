// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 10
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 7
 * NUMBER: 8
 * DESCRIPTION: copy with defaults
 */

open class B(open val a: Int, open val b: Any) {
    override fun hashCode(): Int {
        return 555
    }
}

data class A(override val a: Int = 1, override val b: Any = String) : B(a, b)

fun box(): String {
    val x: A = A(a = 1, String.Companion)
    val y = x.copy()
    if (y.a == x.a && y.b == x.b) {
        return "OK"
    } else return "nok"
}

