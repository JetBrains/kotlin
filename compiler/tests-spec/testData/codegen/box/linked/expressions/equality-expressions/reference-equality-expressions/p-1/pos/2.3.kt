// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, equality-expressions, reference-equality-expressions -> paragraph 1 -> sentence 2
 * RELEVANT PLACES:  expressions, built-in-types-and-their-semantics, kotlin.unit -> paragraph 1 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: check if two values are non-equal (!==) by reference
 */

fun box(): String {
    val u1 = "boo"
    val u2 = "foo"
    if (u1 !== u2) {
        return ("OK")
    }
    return ("NOK")
}