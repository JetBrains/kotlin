// !LANGUAGE: -ForbidReferencingToUnderscoreNamedParameterOfCatchBlock
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER -UNUSED_EXPRESSION
// FULL_JDK

fun foo() {
    try {
        TODO()
    } catch (_: Exception) {
        `_`.stackTrace
    }
    try {
        TODO()
    } catch (_: Exception) {
        val x = {
            val x2 = {
                val x3 = { y: Int ->
                    val x4 = { _: Int ->
                        `_`
                    }
                    `_`
                }
                `_`
                10
            }
            fun bar(x: Exception = `_`) {}
            class Bar(`_`: Exception = `_`) {
                inner class Bar2(x: Exception = `_`) { }
            }
        }
    } catch (_: Exception) {
        `_`.stackTrace
        val y1 = _
        val y2 = (`_`)
    }
    try {
        TODO()
    } catch (_: Exception) {
        try {
            TODO()
        } catch (x: Exception) {
            `_`.stackTrace
        }
    }
    val boo1 = { `_`: Exception ->
        try {
            TODO()
        } catch (x: Exception) {
            `_`.stackTrace
        }
    }
    val boo2 = { _: Exception ->
        try {
            TODO()
        } catch (x: Exception) {
            `_`.stackTrace
        }
    }
}
