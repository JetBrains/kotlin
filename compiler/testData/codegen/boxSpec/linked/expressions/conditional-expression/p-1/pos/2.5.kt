
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * NUMBER: 5
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */


fun box() : String{
    if (get(null)?: if (get(false)!!) false else get(true)!!)
        return "OK"
    return "NOK"
}

fun get(b: Boolean?): Boolean? = b
