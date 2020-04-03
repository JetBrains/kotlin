// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: expressions, jump-expressions, return-expressions -> paragraph 1 -> sentence 1
 * RELEVANT PLACES: expressions, jump-expressions, return-expressions -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:
 */

fun box(): String {
    try {
        fooCase1()
    }catch (e: NotImplementedError){
        return "NOK"
    }
    return "OK"
}

class Case1

fun fooCase1(): Case1 {
    val x = Case1()
    return x
    TODO()
}