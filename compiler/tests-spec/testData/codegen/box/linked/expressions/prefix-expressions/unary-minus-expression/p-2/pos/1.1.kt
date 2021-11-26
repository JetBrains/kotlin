// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, prefix-expressions, unary-minus-expression -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: expressions, prefix-expressions, unary-minus-expression -> paragraph 1 -> sentence 2
 * expressions, prefix-expressions, unary-minus-expression -> paragraph 1 -> sentence 1
 * expressions, prefix-expressions, unary-minus-expression -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: exactly the same as A.unaryMinus() where unaryMinus is a valid operator function available in the current scope.
 */

class A(var a: Int) {
    var isCalled = false
    operator fun unaryMinus(): A {
        isCalled = true
        return A(a)
    }
}

fun box(): String {
    val a = A(-1)
    val a1 = -a
    if (!a1.isCalled && a.isCalled) {
        return "OK"
    }
    return "NOK"
}
