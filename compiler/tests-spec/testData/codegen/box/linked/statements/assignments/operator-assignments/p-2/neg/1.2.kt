// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 2
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 3
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A += B is exactly the same as A.plusAssign(B) or A = A.plus(B) (applied in order)
 * EXCEPTION: compiletime
 */


class B(var a: Int) {
    var plus = false
    var plusAssign = false

    operator fun plus(value: Int): B {
        plus = true
        return B(a + value)
    }

}

fun box() {
    val b = B(1)
    b += 1 //error
}