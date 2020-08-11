// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc%2B0.3-603
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 10
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 11
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 12
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A /= B is exactly the same as A.divAssign(B) or A = A.div(B) (applied in order)
 */


class B(var a: Int) {
    var div = false

    operator fun div(value: Int): B {
        div = true
        a= a / value
        return this
    }

}

fun box(): String {
    var b = B(1)
    b /= 1

    if (b.div)
        return "OK"
    return "NOK"
}