// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: statements, assignments, operator-assignments -> paragraph 2 -> sentence 1
 * RELEVANT PLACES: statements, assignments, operator-assignments -> paragraph 2 -> sentence 2
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 3
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A += B is exactly the same as A.plusAssign(B) or A = A.plus(B) (applied in order)
 */


class B(var a: Int) {
    var plus = false

    operator fun plus(value: Int): B {
        plus = true
        a= a + value
        return this
    }

}

fun box(): String {
    var b = B(1)
    b += 1

    if (b.plus && b.a == 2)
        return "OK"
    return "NOK"
}