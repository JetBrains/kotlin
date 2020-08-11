// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 2
 * declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 3
 * declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 4
 * declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 5
 * declarations, classifier-declaration, data-class-declaration -> paragraph 3 -> sentence 1
 * NUMBER: 11
 * DESCRIPTION: equals and hashcode. Class includes regular property declarations in its body
 */

data class A(val a: Int, val b: String) {
    var prop1: Int
    val prop2: Int

    init {
        prop1 = 1;
        prop2 = 1;
    }
}

fun box(): String {
    val x: A = A(1, "str")
    val y: A = A(1, "str")

    x.prop1 = 2
    y.prop1 = 3
    if (x.equals(y)) {
        if (x.a == y.a && x.b == y.b) {
            if (x.hashCode() == y.hashCode())
                return "OK"
            else return "hashCode are not equal"
        } else return "props are not equal"
    } else return "not equals"
}