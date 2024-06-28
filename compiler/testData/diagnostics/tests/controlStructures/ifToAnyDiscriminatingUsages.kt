// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun println() {}
fun foo(x: Any) {}
fun <T> fooGeneric(x: T) {}

fun testResultOfLambda1() =
        run {
            if (true) 42 else println()
        }

fun testResultOfLambda2() =
        run {
            if (true) 42 else if (true) 42 else println()
        }

fun testResultOfAnonFun1() =
        run(fun () =
                if (true) 42
                else println()
        )

fun testResultOfAnonFun2() =
        run(fun () {
            if (true) 42 else println()
        })

fun testReturnFromAnonFun() =
        run(fun () {
            return <!TYPE_MISMATCH!>if (true) 42 else println()<!>
        })

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>testReturn1<!>() =
        run {
            return <!TYPE_MISMATCH!>if (true) <!IMPLICIT_CAST_TO_ANY!>42<!>
                   else <!IMPLICIT_CAST_TO_ANY!>println()<!><!>
        }

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>testReturn2<!>() =
        run {
            return <!TYPE_MISMATCH!>if (true) <!IMPLICIT_CAST_TO_ANY!>42<!>
                   else if (true) <!IMPLICIT_CAST_TO_ANY!>42<!>
                   else <!IMPLICIT_CAST_TO_ANY!>println()<!><!>
        }

fun testUsage1() =
        if (true) <!IMPLICIT_CAST_TO_ANY!>42<!>
        else <!IMPLICIT_CAST_TO_ANY!>println()<!>

fun testUsage2() =
        foo(if (true) 42 else println())

fun testUsage2Generic() =
        fooGeneric(if (true) 42 else println())

val testUsage3 =
        if (true) <!IMPLICIT_CAST_TO_ANY!>42<!>
        else <!IMPLICIT_CAST_TO_ANY!>println()<!>

val testUsage4: Any get() =
        if (true) 42 else println()
