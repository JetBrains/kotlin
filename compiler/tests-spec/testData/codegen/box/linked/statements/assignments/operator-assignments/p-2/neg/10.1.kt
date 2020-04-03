// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: statements, assignments, operator-assignments -> paragraph 2 -> sentence 10
 * RELEVANT PLACES: statements, assignments, operator-assignments -> paragraph 2 -> sentence 11
 * NUMBER: 1
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

    operator fun divAssign(value: Int) {
        divAssign = true
        a = a / value
    }
}

fun box() {
    var b = B(1)
    b /= 1 //error
}