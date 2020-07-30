// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 10
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 7
 * NUMBER: 4
 * DESCRIPTION: copy with defaults
 */
open class B(open val a: Int, open val b: Any)
data class A(
    override val a: Int = 1,
    override val b: C = C(
        3.0,
        listOf(D(1, 1), D(2, 2))
    )
) : B(a, b)

data class C(val c1: Double = 2.0, var c2: List<D>)
class D(val d1: Int, d2: Int)

fun box(): String {
    val x = A(1)
    val y = x.copy()

    if (y.a == x.a && y.b == x.b) {
        return "OK"
    } else return "nok"
}


