// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments, operator-assignments -> paragraph 2 -> sentence 13
 * PRIMARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 14
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 15
 * statements, assignments, operator-assignments -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A %= B is exactly the same as A.remAssign(B) or A = A.rem(B) (applied in order)
 */


class B(var a: Int) {
    var rem = false

    operator fun rem(value: Int): B {
        rem = true
        a= a % value
        return this
    }

}

fun box(): String {
    var b = B(1)
    b %= 1

    if (b.rem)
        return "OK"
    return "NOK"
}