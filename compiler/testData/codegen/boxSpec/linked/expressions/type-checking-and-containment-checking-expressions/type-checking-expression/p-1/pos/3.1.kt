// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, type-checking-and-containment-checking-expressions, type-checking-expression -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION:checks whether the runtime type of E is not a subtype of T for !is operator.
 */

fun box(): String {
    val bar = ::boo
    val bar1 = ::exit
    if (bar !is () -> Nothing && bar1 !is (Nothing) -> Nothing) return "NOK"

    return "OK"
}

private fun boo(): Nothing = TODO()
private fun exit(x: () -> Any?): Nothing = throw Exception(x().toString())
private fun enter(): Nothing = TODO()

