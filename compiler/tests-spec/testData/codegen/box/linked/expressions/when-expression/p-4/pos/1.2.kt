// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 4 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION:  it is possible to  replace the else condition with an always-true condition (Boolean)
 */

fun box(): String {
    val b = true
    val when2 = when (b) {
        false -> { "NOK" }
        !false -> { "OK" }
        !false -> { "NOK" }
        else -> { "NOK" }
    }
    return  when2
}
