// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: expressions, jump-expressions, continue-expression -> paragraph 3 -> sentence 1
 * RELEVANT PLACES: expressions, jump-expressions, continue-expression -> paragraph 1 -> sentence 1
 * expressions, jump-expressions, continue-expression -> paragraph 1 -> sentence 2
 * expressions, jump-expressions, continue-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A simple continue expression, specified using the continue keyword, which continue-jumps to the innermost loop statement in the current scope
 */

fun box(): String {
    val inputList = listOf(1, 2, 3)
    var list1 = mutableListOf<Any>()
    for (it in listOf(1, 2, 3)){
        for (it1 in listOf("1.", "2.", "3.")) {
            if (true) continue
        }
        list1.add(it)
    }
    if (list1.containsAll(inputList))
        return "OK"
    else return "NOK"
}