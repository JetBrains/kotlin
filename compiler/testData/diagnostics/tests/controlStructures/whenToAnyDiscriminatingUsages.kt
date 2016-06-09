// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

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
            return <!TYPE_MISMATCH!>when {
                true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
                else -> <!IMPLICIT_CAST_TO_ANY!>println()<!>
            }<!>
        }

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>testReturn2<!>() =
        run {
            return <!TYPE_MISMATCH!>when {
                true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
                else ->
                    when {
                        true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
                        else -> <!IMPLICIT_CAST_TO_ANY!>println()<!>
                    }
            }<!>
        }

fun testUsage1() =
        when {
            true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
            else -> <!IMPLICIT_CAST_TO_ANY!>println()<!>
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
            true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
            else -> <!IMPLICIT_CAST_TO_ANY!>println()<!>
        }

val testUsage4 =
        when {
            true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
            true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
            true -> <!IMPLICIT_CAST_TO_ANY!>42<!>
            else -> <!IMPLICIT_CAST_TO_ANY!>println()<!>
        }

val testUsage5: Any get() =
        when {
            true -> 42
            else -> println()
        }