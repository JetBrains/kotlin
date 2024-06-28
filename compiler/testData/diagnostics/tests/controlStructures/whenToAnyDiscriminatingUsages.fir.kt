// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun println() {}
fun foo(x: Any) {}
fun <T> fooGeneric(x: T) {}

fun testResultOfLambda1() =
        run {
            when {
                true -> 42
                else -> println()
            }
        }

fun testResultOfLambda2() =
        run {
            when {
                true -> 42
                else ->
                    when {
                        true -> 42
                        else -> println()
                    }
            }
        }

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>testReturn1<!>() =
        run {
            return <!RETURN_TYPE_MISMATCH!>when {
                true -> 42
                else -> println()
            }<!>
        }

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>testReturn2<!>() =
        run {
            return <!RETURN_TYPE_MISMATCH!>when {
                true -> 42
                else ->
                    when {
                        true -> 42
                        else -> println()
                    }
            }<!>
        }

fun testUsage1() =
        when {
            true -> 42
            else -> println()
        }

fun testUsage2() =
        foo(when {
                true -> 42
                else -> println()
            })

fun testUsage2Generic() =
        fooGeneric(when {
                       true -> 42
                       else -> println()
                   })

val testUsage3 =
        when {
            true -> 42
            else -> println()
        }

val testUsage4 =
        when {
            true -> 42
            true -> 42
            true -> 42
            else -> println()
        }

val testUsage5: Any get() =
        when {
            true -> 42
            else -> println()
        }
