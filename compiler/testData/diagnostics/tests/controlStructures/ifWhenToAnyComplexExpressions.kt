// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun println() {}
fun foo(x: Any) {}
fun <T> fooGeneric(x: T) {}

fun testMixedIfAndWhen() =
        if (true)
            when {
                true -> if (true) <!IMPLICIT_CAST_TO_ANY!>42<!>
                        else <!IMPLICIT_CAST_TO_ANY!>1<!>
                true -> if (true) <!IMPLICIT_CAST_TO_ANY!>42<!>
                        else <!IMPLICIT_CAST_TO_ANY!>println()<!>
                else -> <!INVALID_IF_AS_EXPRESSION!>if (true) <!IMPLICIT_CAST_TO_ANY!>println()<!><!>
            }
        else <!IMPLICIT_CAST_TO_ANY!>println()<!>

fun testWrappedExpressions() =
        if (true) {
            println()
            <!INVALID_IF_AS_EXPRESSION!>if (true) {
                println()
                if (true) {
                    <!IMPLICIT_CAST_TO_ANY!>println()<!>
                }
                else <!IMPLICIT_CAST_TO_ANY!>{}<!>
            }<!>
        }
        else {
            (((<!IMPLICIT_CAST_TO_ANY!>((42)) + 1<!>)))
        }