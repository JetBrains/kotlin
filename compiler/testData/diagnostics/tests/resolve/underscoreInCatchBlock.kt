// LANGUAGE: -ForbidReferencingToUnderscoreNamedParameterOfCatchBlock
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER -UNUSED_EXPRESSION
// FULL_JDK

fun foo() {
    try {
        TODO()
    } catch (_: Exception) {
        <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>.stackTrace
    }
    try {
        TODO()
    } catch (_: Exception) {
        val x = {
            val x2 = {
                val x3 = { y: Int ->
                    val x4 = { _: Int ->
                        <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>
                    }
                    <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>
                }
                <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>
                10
            }
            fun bar(x: Exception = <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>) {}
            class Bar(`_`: Exception = <!UNINITIALIZED_PARAMETER!>`_`<!>) {
                inner class Bar2(x: Exception = <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>) { }
            }
        }
    } catch (_: Exception) {
        <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>.stackTrace
        val y1 = <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER, UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
        val y2 = (<!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>)
    }
    try {
        TODO()
    } catch (_: Exception) {
        try {
            TODO()
        } catch (x: Exception) {
            <!RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER!>`_`<!>.stackTrace
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
            <!UNRESOLVED_REFERENCE!>`_`<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>stackTrace<!>
        }
    }
}
