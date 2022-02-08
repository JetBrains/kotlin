
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: expressions, conditional-expression -> paragraph 4 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */

fun box(): String {
    if (false) return "NOK"
    return "OK"
}