// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-220
 * MAIN LINK: expressions, prefix-expressions, logical-not-expression -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: expressions, prefix-expressions, logical-not-expression -> paragraph 1 -> sentence 1
 * expressions, prefix-expressions, logical-not-expression -> paragraph 1 -> sentence 2
 * expressions, prefix-expressions, logical-not-expression -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:
 */

class A(var a: Int) {
    var isCalled = false
    operator fun not(): A {
        isCalled = true
        return A(a)
    }
}

fun box(): String {
    val a = A(-1)
    val a1 = !a
    if (!a1.isCalled && a.isCalled) {
        return "OK"
    }
    return "NOK"
}
