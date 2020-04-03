// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: statements, assignments, operator-assignments -> paragraph 2 -> sentence 10
 * RELEVANT PLACES: statements, assignments, operator-assignments -> paragraph 2 -> sentence 11
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 12
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A /= B is exactly the same as A.divAssign(B) or A = A.div(B) (applied in order)
 * EXCEPTION: compiletime
 */


class B(var a: Int) {
    var div = false
    var divAssign = false

    operator fun div(value: Int): B {
        div = true
        return B(a / value)
    }

}

fun box() {
    val b = B(1)
    b /= 1 //error
}