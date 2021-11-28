// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, equality-expressions, reference-equality-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION: check if values are non-equal (!==) by reference
 */

fun box(): String {
    val u1 = "foo"
    val byteArray = "foo".toByteArray(Charsets.UTF_8)
    val u2 = byteArray.toString()
    val u3 = byteArray.toString()
    if (u1 !== u2 && u1 !== u3 && u2 !== u3 &&
        u2 !== u1 && u3 !== u1 && u3 !== u2
    ) {
        return ("OK")
    }
    return ("NOK")
}