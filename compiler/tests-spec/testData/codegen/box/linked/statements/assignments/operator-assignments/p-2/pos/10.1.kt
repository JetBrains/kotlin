// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 10
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 11
 * NUMBER: 1
 * DESCRIPTION: A /= B is exactly the same as A.divAssign(B) or A = A.div(B) (applied in order)
 */

class B(var a: Int) {
    var div = false
    var divAssign = false

    operator fun div(value: Int): B {
        div = true
        return B(a / value)
    }

    operator fun divAssign(value: Int) {
        divAssign = true
        a = a / value
    }
}

fun box(): String {
    val b = B(1)
    b /= 1

    if (!b.div && b.divAssign)
            return "OK"
    return "NOK"
}