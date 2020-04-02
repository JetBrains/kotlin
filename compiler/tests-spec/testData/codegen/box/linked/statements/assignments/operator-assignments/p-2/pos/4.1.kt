// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * PLACE: statements, assignments, operator-assignments -> paragraph 2 -> sentence 4
 * RELEVANT PLACES: statements, assignments, operator-assignments -> paragraph 2 -> sentence 5
 * NUMBER: 1
 * DESCRIPTION: A -= B is exactly the same as A.minusAssign(B) or A = A.minus(B) (applied in order)
 */

class B(var a: Int) {
    var minus = false
    var minusAssign = false

    operator fun minus(value: Int): B {
        minus = true
        return B(a - value)
    }

    operator fun minusAssign(value: Int) {
        minusAssign = true
        a = a - value
    }
}

fun box(): String {
    val b = B(1)
    b -= 1

    if (!b.minus && b.minusAssign && b.a == 0)
        return "OK"
    return "NOK"
}