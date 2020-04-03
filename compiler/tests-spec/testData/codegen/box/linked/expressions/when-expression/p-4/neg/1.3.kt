// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * PLACE: expressions, when-expression -> paragraph 4 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION:  it is possible to  replace the else condition with an always-true condition (Boolean)
 * EXCEPTION: compiletime
 */

fun box(): String {
    val a = false
    val when2 = when (a) {
        false -> { "OK" }
        false -> { "NOK" }
    }
    return when2
}