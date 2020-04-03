// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: expressions, jump-expressions, break-expression -> paragraph 3 -> sentence 2
 * RELEVANT PLACES: expressions, jump-expressions, break-expression -> paragraph 1 -> sentence 1
 * expressions, jump-expressions, break-expression -> paragraph 1 -> sentence 2
 * expressions, jump-expressions, break-expression -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: A labeled break expression, denoted break@Loop, where Loop is a label of a labeled loop statement L, which break-jumps to the loop L.
 */


fun box(): String {
    val inputList = listOf(0, 1, 2)
    var x = 0
    var y = 0
    var list1 = mutableListOf<Any>()
    while (x < 3) {
        label@ while  (y< 3) {
            y++
            if (true) break@label
        }
        list1.add(x++)
    }
    if (list1.containsAll(inputList))
        return "OK"
    else return "NOK"
}