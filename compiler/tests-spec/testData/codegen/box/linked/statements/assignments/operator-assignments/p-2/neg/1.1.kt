// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 2
 * NUMBER: 1
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

    operator fun plusAssign(value: Int) {
        plusAssign = true
        a = a + value
    }
}

fun box() {
    var b = B(1)
    b += 1 //error
}