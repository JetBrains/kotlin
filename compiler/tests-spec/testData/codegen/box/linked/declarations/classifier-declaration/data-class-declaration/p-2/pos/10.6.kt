// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 10
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 7
 * NUMBER: 6
 * DESCRIPTION: copy with defaults
 */


open class B(open val a: Int, open val b: Any) {
    override fun hashCode(): Int {
        return 555
    }
}


data class A(override val a: Int = 1, override val b: B = B_VAL) : B(a, b)

val B_VAL = B(8, "string")

fun box(): String {
    val x = A(a = 2)
    val y = x.copy()
    if (y.a == x.a && y.b == x.b) {
        return "OK"
    } else return "nok"
}
