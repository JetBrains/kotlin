// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 13
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 14
 * NUMBER: 1
 * DESCRIPTION: A %= B is exactly the same as A.remAssign(B) or A = A.rem(B) (applied in order)
 * EXCEPTION: compiletime
 */

class B(var a: Int) {
    var rem = false
    var remAssign = false

    operator fun rem(value: Int): B {
        rem = true
        return B(a % value)
    }

    operator fun remAssign(value: Int) {
        remAssign = true
        a = a % value
    }
}

fun box() {
    var b = B(1)
    b %= 1 //error
}