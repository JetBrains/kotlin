// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: expressions, jump-expressions, return-expressions -> paragraph 1 -> sentence 1
 * RELEVANT PLACES:  expressions, jump-expressions, return-expressions -> paragraph 1 -> sentence 2
 * expressions, jump-expressions, return-expressions -> paragraph 5 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION:
 */
val flag = false
fun box() : String{
    val x = foo()
    if (x is kotlin.Unit && !flag)
        return "OK"
    return "NOK"
}

fun foo() {
    return
    1
    flag = true
    val x = ""
}