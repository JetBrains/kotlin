// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, equality-expressions, reference-equality-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 5
 * DESCRIPTION: two values are equal (===) or non-equal (!==) by reference
 */

fun box(): String {
    val c1 = c
    val c2 = c
    val v1 = v
    val v2 = v
    if (c2 === c1 && c1 === c2 && c1 === c && c2 === c &&
        v2 === v1 && v1 === v2 && v1 === v && v2 === v &&
        v2 === c1 && v1 === c2 && v1 === c && v2 === c &&
        c2 === v1 && c1 === v2 && c1 === v && c2 === v
    ) {
        return ("OK")
    }
    return ("NOK")
}

const val c = 1000
const val v = 1000