// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 12
 * NUMBER: 1
 * DESCRIPTION: generated component function has the same type as this property and returns the value of this property
 */

data class A(val a: Int, val b: String)

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