// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, prefix-expressions, prefix-decrement-expression -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check the result of dec() is assigned to A, the return type of inc must be a subtype of A.
 */


fun box(): String {
    var res = BDecrement(1)
    val res1: Any = --res

    return if (res1 is BDecrement && res.i == -1 && res.j == 0 && res1.i == -1 && res1.j == 0) {
        val res2: Any? = --res
        if (res2 is BDecrement && res.i == -2 && res.j == -1 && res2.i == -2 && res2.j == -1)
            "OK"
        else "NOK"
    } else
        "NOK"
}

data class BDecrement(var j: Int) : ADecrement() {
    override operator fun dec(): BDecrement {
        super.dec()
        this.j -= 1
        return this
    }
}

open class ADecrement() {
    var i = 0
    open operator fun dec(): ADecrement {
        this.i -= 1
        return this
    }
}