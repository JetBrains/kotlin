// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: statements, assignments, simple-assignments -> paragraph 6 -> sentence 1
 * RELEVANT PLACES: statements, assignments, simple-assignments -> paragraph 7 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:
 */


class A() {
    val list = arrayListOf(1, 2, 3)
    operator fun set(a: Int, value: Int) {
        this.list.add(a, value)
    }

    operator fun set(a: Int, b: Int, value: Int) {
        this.list.add(a, value)
        this.list.add(b, value)
    }
}

fun box(): String {
    val a = A()
    a[0] = 0

    val b = A()
    b[0, 2] = 0

    if (a.list == arrayListOf(0, 1, 2, 3) &&
        b.list == arrayListOf(0, 1, 0, 2, 3)
    )
        return "OK"
    return "NOK"
}