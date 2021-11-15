// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 4
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 5
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A -= B is exactly the same as A.plusAssign(B) or A = A.minus(B) (applied in order)
 */


class B(var a: Int) {
    var minus = false

    operator fun minus(value: Int): B {
        minus = true
        a= a - value
        return this
    }

}

fun box(): String {
    var b = B(1)
    b -= 1

    if (b.minus && b.a == 0)
        return "OK"
    return "NOK"
}