// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 12
 * PRIMARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 3 -> sentence 1
 * NUMBER: 18
 * DESCRIPTION:  generated component function has the same type as this property and returns the value of this property. Class includes regular property declarations in its body
 */

open class B(open val a: Int, open val b: Any) {
    override fun hashCode(): Int {
        return 555
    }
}

data class A(override val a: Int = 1, override val b: Any = "String") : B(a, b){
    var prop1: Set<Any>;

    init {
        prop1 = emptySet();
    }
}

fun box(): String {
    val x: A = A(b= String.Companion, a = 1)
    if (x.component1() is Int
        && x.component2() is String.Companion
        && x.component1() == x.a &&
        x.component2() == x.b
    ) {
        return "OK"
    } else return "nok"
}

