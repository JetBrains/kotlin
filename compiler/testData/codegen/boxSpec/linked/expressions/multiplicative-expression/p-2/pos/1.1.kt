// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, multiplicative-expression -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: expressions, multiplicative-expression -> paragraph 1 -> sentence 1
 * expressions, multiplicative-expression -> paragraph 1 -> sentence 2
 * overloadable-operators -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A * B is exactly the same as A.times(B)
 */

class A(var a: Int) {
    var isCalled = false
    var isCalledInt = false
    operator fun times(o: Int): A {
        isCalledInt = true
        a *= o
        return this
    }

    operator fun times(o: A): A {
        isCalled = true
        return A(a * o.a)
    }
}

fun box(): String {
    val a1 = A(-1)
    val a2 = A(5)
    val x = a1 * a2

    if (a1.isCalled && !a2.isCalled && x.a == -5) {
        val a3 = A(3)
        val y = a3 * 2
        if (a3.isCalledInt && y.a == 6)
            return "OK"
    }
    return "NOK"
}