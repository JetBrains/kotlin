// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-253
 * MAIN LINK: statements, loop-statements, while-loop-statement -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: statements, loop-statements, while-loop-statement -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: while-loop-statement evaluates the loop condition expression before evaluating the loop body.
 */

fun box(): String {
    var x = 1;
    var cond = true
    while (cond) {
        x++
        cond = false
    }
    if (x == 2)
        return "OK"
    return "NOK"
}