// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 12
 * PRIMARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 3 -> sentence 1
 * NUMBER: 11
 * DESCRIPTION: generated component function has the same type as this property and returns the value of this property. Class includes regular property declarations in its body
 */

data class A(val a: Int, val b: String){
    var prop1: Set<Any>;

    init {
        prop1 = emptySet();
    }
}

fun box(): String {
    val x: A = A(1, "str")

    if (x.component1() is Int
        && x.component2() is String
        && x.component1() == 1 &&
        x.component2() == "str"
    ) {
        return "OK"
    } else return "nok"
}