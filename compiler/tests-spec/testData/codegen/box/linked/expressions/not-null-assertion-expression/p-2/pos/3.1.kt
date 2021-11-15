// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-220
 * MAIN LINK: expressions, not-null-assertion-expression -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: If the evaluation result of e is not equal to null, the result of e!! is the evaluation result of e.
 */

fun box(): String {

    val x: String? = "str"
    try {
      val y = x!!
    }catch (e: java.lang.NullPointerException){
        return "NOK"
    }
    return "OK"
}