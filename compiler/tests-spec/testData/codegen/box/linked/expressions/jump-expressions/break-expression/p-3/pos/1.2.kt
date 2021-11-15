// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: expressions, jump-expressions, break-expression -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: expressions, jump-expressions, break-expression -> paragraph 1 -> sentence 1
 * expressions, jump-expressions, break-expression -> paragraph 1 -> sentence 2
 * expressions, jump-expressions, break-expression -> paragraph 2 -> sentence 1
 * statements, loop-statements, while-loop-statement -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: A simple break expression, specified using the continue keyword, which break-jumps to the innermost loop statement in the current scope
 */

fun box(): String {
    val inputList = listOf(0, 1, 2)
    var x = 0
    var y = 0
    var list1 = mutableListOf<Any>()
    while (x < 3) {
        while  (y< 3) {
            y++
            if (true) break
        }
        list1.add(x++)
    }
    if (list1.containsAll(inputList))
        return "OK"
    else return "NOK"
}