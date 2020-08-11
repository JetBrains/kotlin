// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: A += B is exactly the same as A.plusAssign(B) or A = A.plus(B) (applied in order)
 */

class B(var a: Int) {
    var plus = false
    var plusAssign = false

    operator fun plus(value: Int): B {
        plus = true
        return B(a + value)
    }

    operator fun plusAssign(value: Int) {
        plusAssign = true
        a = a + value
    }
}

fun box(): String {
    val b = B(1)
    b += 1

    if (!b.plus && b.plusAssign && b.a == 2)
            return "OK"
    return "NOK"
}