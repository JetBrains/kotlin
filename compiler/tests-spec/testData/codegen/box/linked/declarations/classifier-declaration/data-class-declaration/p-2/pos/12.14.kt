// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 12
 * PRIMARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 3 -> sentence 1
 * NUMBER: 14
 * DESCRIPTION:  generated component function has the same type as this property and returns the value of this property. Class includes regular property declarations in its body
 */
open class B(open val a: Int, open val b: Any)
data class A(
    override val a: Int = 1,
    override val b: C = C(
        3.0,
        listOf(D(1, 1), D(2, 2))
    )
) : B(a, b){
    var prop1: Set<Any>;

    init {
        prop1 = emptySet();
    }
}

class C(val c1: Double = 2.0, var c2: List<D>)
class D(val d1: Int, d2: Int)

fun box(): String {
    val x = A(2)

    if (x.component1() is Int
        && x.component2() is C
        && x.component1() == x.a
        && x.component1() == 2
        && x.component2() == x.b
    ) {
        return "OK"
    } else return "nok"
}


