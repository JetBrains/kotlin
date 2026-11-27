// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.unit -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: expressions, equality-expressions, reference-equality-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: todo
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
    if (u1 === u2 && u1 === u3 && u2 === u3) {
        return ( "OK")
    }
    return ( "NOK")
}
