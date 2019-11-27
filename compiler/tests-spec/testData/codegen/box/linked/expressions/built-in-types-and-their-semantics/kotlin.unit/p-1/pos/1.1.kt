// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.unit -> paragraph 1 -> sentence 1
 * RELEVANT PLACES: expressions, equality-expressions, reference-equality-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check  all values of type kotlin.Unit should reference the same underlying kotlin.Unit object.
 */

fun foo1() {
    return Unit
}

fun foo2(): Unit {
    return Unit
}

fun foo3(): Unit {
}

fun box(): String {
    val u1 = foo1()
    val u2 = foo2()
    val u3 = foo3()
    val u4 = Unit
    if (u1 === u2 && u1 === u3 && u1 === u4
        && u2 === u3 && u2 === u4
        && u3 === u4
    ) {
        return ("OK")
    }
    return ("NOK")
}
