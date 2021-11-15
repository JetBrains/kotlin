// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * NUMBER: 8
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */

fun box(): String {
    var x = 10
    var flag1 = false
    if (x == 10.also { x = 11 }) {
        flag1 = true
    }

    var flag2 = false
    if (x == if (true) {x = 12; 11} else 11) {
        flag2 = true
    }
    var flag3 = false
    if (12.also { x = 13 } == x) {
        flag3 = true
    }

    if (flag1 && flag2 && !flag3)
        return "OK"
    return "NOK"
}