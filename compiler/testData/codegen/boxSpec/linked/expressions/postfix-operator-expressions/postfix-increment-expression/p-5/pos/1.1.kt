// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, postfix-operator-expressions, postfix-increment-expression -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check the result of inc() is assigned to A, the return type of inc must be a subtype of A.
 */


fun box(): String {
    var res = BIncrement(1 )
    val res1: Any = res++
    return if (res1 is BIncrement && res.i == 1 && res.j == 2 && res1.i == 0 && res1.j == 1) {
        "OK"
    } else
        "NOK"
}


data class BIncrement(var j: Int, var k: Int = 0) : AIncrement(k) {
    override operator fun inc(): BIncrement {
        super.inc()
        return BIncrement(j + 1, this.i+1)
    }
}

open class AIncrement(var i: Int = 0) {
    open operator fun inc(): AIncrement {
        return AIncrement(i + 1)
    }
}