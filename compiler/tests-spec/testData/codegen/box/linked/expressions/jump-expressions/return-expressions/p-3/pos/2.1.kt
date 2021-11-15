// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, jump-expressions, return-expressions -> paragraph 3 -> sentence 2
 * PRIMARY LINKS: expressions, jump-expressions, return-expressions -> paragraph 2 -> sentence 1
 * expressions, jump-expressions, return-expressions -> paragraph 3 -> sentence 3
 * expressions, jump-expressions, return-expressions -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If several declarations match one name, the return is considered to be from the nearest matching function;
 */

fun box(): String {
    val inputList = listOf(1, 2, 3)
    var list1 = mutableListOf<Any>()
    inputList.forEach mark@{
        list1.add(it)
        listOf("1.", "2.", "3.").forEach mark@{
            if (true) return@mark
        }
    }
    if (list1.containsAll(inputList))
        return "OK"
    else return "NOK"
}