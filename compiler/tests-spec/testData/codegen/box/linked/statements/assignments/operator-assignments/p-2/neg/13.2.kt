// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: statements, assignments, operator-assignments -> paragraph 2 -> sentence 13
 * RELEVANT PLACES: statements, assignments, operator-assignments -> paragraph 2 -> sentence 14
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 15
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
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

}

fun box() {
    val b = B(1)
    b %= 1 //error
}