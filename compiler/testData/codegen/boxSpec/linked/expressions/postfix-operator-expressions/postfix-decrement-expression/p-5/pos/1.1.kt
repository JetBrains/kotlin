// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, postfix-operator-expressions, postfix-decrement-expression -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check the result of dec() is assigned to A, the return type of dec must be a subtype of A.
 */


fun box(): String {
    var res = B(1 )
    val res1: Any = res--
    return if (res1 is B && res.i == -1 && res.j == 0 && res1.i == 0 && res1.j == 1) {
        "OK"
    } else
        "NOK"
}


data class B(var j: Int, var k: Int = 0) : A(k) {
    override operator fun dec(): B {
        super.dec()
        return B(j - 1, this.i-1)
    }
}

open class A(var i: Int = 0) {
    open operator fun dec(): A {
        return A(i - 1)
    }
}