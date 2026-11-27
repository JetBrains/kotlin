// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-220
 * MAIN LINK: expressions, indexing-expressions -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: expressions, indexing-expressions -> paragraph 1 -> sentence 1
 * expressions, indexing-expressions -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A[I_0,I_1,...,I_N] is exactly the same as A.get(I_0,I_1,...,I_N), where get is a valid operator function available in the current scope
 */

class A1(val a: Int = 0) {

    operator fun get(x: Int): A1 {
        return A1(x)
    }
}

fun box(): String {
    val a = A1()[9]
    val x = a[1][2][3][4]
    if (x.a == 4 && a.a == 9)
        return "OK"
    return "NOK"
}