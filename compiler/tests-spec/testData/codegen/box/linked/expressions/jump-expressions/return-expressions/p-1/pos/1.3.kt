// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: expressions, jump-expressions, return-expressions -> paragraph 1 -> sentence 1
 * PRIMARY LINKS:  expressions, jump-expressions, return-expressions -> paragraph 1 -> sentence 2
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