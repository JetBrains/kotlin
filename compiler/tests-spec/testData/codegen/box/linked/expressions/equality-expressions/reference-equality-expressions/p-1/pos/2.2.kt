// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, equality-expressions, reference-equality-expressions -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, equality-expressions, reference-equality-expressions -> paragraph 3 -> sentence 3
 * expressions, equality-expressions, reference-equality-expressions -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: check if two values are equal (===) by reference
 */

fun box(): String {
    val u1 = "foo"
    val u2 = "foo"
    if (u1 === u2) {
        return ("OK")
    }
    return ("NOK")
}