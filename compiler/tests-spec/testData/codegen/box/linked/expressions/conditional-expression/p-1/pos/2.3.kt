
// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * RELEVANT PLACES: expressions, conditional-expression -> paragraph 4 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */

fun box(): String {
    if (false) return "NOK"
    return "OK"
}