// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: expressions, jump-expressions, continue-expression -> paragraph 3 -> sentence 2
 * PRIMARY LINKS: expressions, jump-expressions, continue-expression -> paragraph 1 -> sentence 1
 * expressions, jump-expressions, continue-expression -> paragraph 1 -> sentence 2
 * expressions, jump-expressions, continue-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A labeled continue expression, denoted continue@Loop, where Loop is a label of a labeled loop statement L, which continue-jumps to the loop L.
 */

fun box() : String{
    val inputList = listOf(1, 2, 3)
    var list1 = mutableListOf<Any>()
    for (it in listOf(1, 2, 3)) {
        label@ for (it1 in listOf("1.", "2.", "3."))  {
            if (true) continue@label
        }
        list1.add(it)
    }
    if (list1.containsAll(inputList))
        return "OK"
    else return "NOK"
}