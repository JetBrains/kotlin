// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, prefix-expressions, unary-plus-expression -> paragraph 2 -> sentence 1
 * RELEVANT PLACES: expressions, prefix-expressions, unary-plus-expression -> paragraph 1 -> sentence 1
 * expressions, prefix-expressions, unary-plus-expression -> paragraph 1 -> sentence 2
 * expressions, prefix-expressions, unary-plus-expression -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: +A is exactly the same as A.unaryPlus() where unaryMinus is a valid operator function available in the current scope.
 */

class A(var a: Int) {
    var isCalled = false
    operator fun unaryPlus(): A {
        isCalled = true
        return A(a)
    }
}

fun box(): String {
    val a = A(-1)
    val a1 = +a
    if (!a1.isCalled && a.isCalled) {
        return "OK"
    }
    return "NOK"
}
