
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * NUMBER: 4
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */

fun box(): String {
    if (false) else return "OK"
    return "NOK"
}