// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */

fun box(): String {
    if (true) return "OK" else "false branch"
    return "NOK"
}