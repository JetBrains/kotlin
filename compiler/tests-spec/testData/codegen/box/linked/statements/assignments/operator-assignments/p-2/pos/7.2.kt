// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 7
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 8
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 9
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A *= B is exactly the same as A.timesAssign(B) or A = A.times(B) (applied in order)
 */


class B(var a: Int) {
    var times = false

    operator fun times(value: Int): B {
        times = true
        a= a * value
        return this
    }

}

fun box(): String {
    var b = B(2)
    b *= 3

    if (b.times && b.a == 6)
        return "OK"
    return "NOK"
}