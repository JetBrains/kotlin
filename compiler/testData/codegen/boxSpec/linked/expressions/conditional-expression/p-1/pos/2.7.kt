// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, conditional-expression -> paragraph 1 -> sentence 2
 * NUMBER: 7
 * DESCRIPTION: if-expression: check the correct branch is evaluating
 */


fun box(): String {
    val x = if (get(null) ?: if (try {
                TODO()
            } catch (e: NotImplementedError) {
                get(false)!!
            }
        ) false else get(true)!!
    ) get(true) else false
    if (x!!) return "OK"
    return "NOK"
}

fun get(b: Boolean?): Boolean? = b
