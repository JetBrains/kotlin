// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun useSuspendVararg(vararg sfn: suspend () -> Unit) {}

fun testSuspendConversionInVarargElementsSome(
    sf1: suspend () -> Unit,
    f2: () -> Unit,
    sf3: suspend () -> Unit
) {
    <!INAPPLICABLE_CANDIDATE!>useSuspendVararg<!>(sf1, f2, sf3)
}

fun testSuspendConversionInVarargElementsAll(
    f1: () -> Unit,
    f2: () -> Unit,
    f3: () -> Unit
) {
    <!INAPPLICABLE_CANDIDATE!>useSuspendVararg<!>(f1, f2, f3)
}