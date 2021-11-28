// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, prefix-expressions, prefix-increment-expression -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check the result of inc() is assigned to A, the return type of inc must be a subtype of A.
 */

fun box(): String {
    var res = BIncrement(1)
    val res1: Any = ++res

    return if (res1 is BIncrement && res.i == 1 && res.j == 2 && res1.i == 1 && res1.j == 2) {
        val res2: Any? = ++res
        if (res2 is BIncrement && res.i == 2 && res.j == 3 && res2.i == 2 && res2.j == 3)
            "OK"
        else "NOK"
    } else
        "NOK"
}

data class BIncrement(var j: Int) : AIncrement() {
    override operator fun inc(): BIncrement {
        super.inc()
        this.j += 1
        return this
    }
}

open class AIncrement() {
    var i = 0
    open operator fun inc(): AIncrement {
        this.i += 1
        return this
    }
}