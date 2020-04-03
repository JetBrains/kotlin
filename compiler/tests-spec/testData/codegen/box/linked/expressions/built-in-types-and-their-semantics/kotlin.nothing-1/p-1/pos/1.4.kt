// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: check kotlin.Nothing type
 */


fun box(): String {
    val bar = ::exit
    try {
        bar(enter())
    } catch (e: NotImplementedError) {
        return "OK"
    }
    return "NOK"
}


private fun exit(x: () -> Any?): Nothing = throw Exception(x().toString())
private fun enter(): Nothing = TODO()
