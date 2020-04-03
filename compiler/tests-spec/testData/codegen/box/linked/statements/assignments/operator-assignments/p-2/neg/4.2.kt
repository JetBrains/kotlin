// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-253
 * PLACE: statements, assignments, operator-assignments -> paragraph 2 -> sentence 4
 * RELEVANT PLACES: statements, assignments, operator-assignments -> paragraph 2 -> sentence 5
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 6
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A -= B is exactly the same as A.minusAssign(B) or A = A.minus(B) (applied in order)
 * EXCEPTION: compiletime
 */


class B(var a: Int) {
    var minus = false
    var minusAssign = false

    operator fun minus(value: Int): B {
        minus = true
        return B(a + value)
    }

}

fun box(){
    val b = B(1)
    b -= 1 //error
}