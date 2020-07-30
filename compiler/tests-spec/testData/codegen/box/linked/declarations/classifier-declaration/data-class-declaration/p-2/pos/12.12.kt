// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 12
 * PRIMARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 3 -> sentence 1
 * NUMBER: 12
 * DESCRIPTION: generated component function has the same type as this property and returns the value of this property. Class includes regular property declarations in its body
 */

open class B(open val a: Int, open val b: Any)
data class A(override val a: Int, override val b: C) : B(a, b){
    var prop1: Set<Any>;

    init {
        prop1 = emptySet();
    }
}
class C

fun box(): String {
    val c = C()
    val x = A(1, c)

    if (x.component1() is Int
        && x.component2() is C
        && x.component1() == 1 &&
        x.component2() == c
    ) {
        return "OK"
    } else return "nok"
}