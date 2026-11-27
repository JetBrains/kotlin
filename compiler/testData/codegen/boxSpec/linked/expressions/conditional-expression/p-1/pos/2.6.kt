
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * NUMBER: 6
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */

fun box() : String{
    if (get(null)?: if (get(true)!!) false else get(true)!!)
        return "NOK"
    return "OK"
}

fun get(b: Boolean?): Boolean? = b
