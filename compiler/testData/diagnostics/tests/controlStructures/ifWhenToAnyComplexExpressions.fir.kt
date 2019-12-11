// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun println() {}
fun foo(x: Any) {}
fun <T> fooGeneric(x: T) {}

fun testMixedIfAndWhen() =
        if (true)
            when {
                true -> if (true) 42
                else 1
                    true -> if (true) 42
                else println()
                else -> if (true) println()
            }
        else println()

fun testWrappedExpressions() =
        if (true) {
            println()
            if (true) {
                println()
                if (true) {
                    println()
                }
                else {}
            }
        }
        else {
            (((((42)) + 1)))
        }