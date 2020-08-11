// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, jump-expressions, return-expressions -> paragraph 3 -> sentence 3
 * PRIMARY LINKS: expressions, jump-expressions, return-expressions -> paragraph 2 -> sentence 1
 * expressions, jump-expressions, return-expressions -> paragraph 3 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: If return@Context is inside a lambda expression body, the name of the function using this lambda expression as its argument may be used as Context to refer to the lambda literal itself
 */

fun box(): String {
    val inputList = listOf(1, 2, 3)
    var list1 = mutableListOf<Any>()
    inputList.forEach {
        list1.add(it)
        listOf("1.", "2.", "3.").forEach {
            if (true) return@forEach
        }
    }
    if (list1.containsAll(inputList))
        return "OK"
    else return "NOK"

}


