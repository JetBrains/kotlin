// SKIP_KT_DUMP
// !LANGUAGE: +SuspendConversion

fun useSuspendVararg(vararg sfn: suspend () -> Unit) {}

fun testSuspendConversionInVarargElementsSome(
    f1: () -> Unit,
    sf2: suspend () -> Unit,
    f3: () -> Unit,
) {
    useSuspendVararg(f1, sf2, f3)
}

fun testSuspendConversionInVarargElementsAll(
    f1: () -> Unit,
    f2: () -> Unit,
    f3: () -> Unit
) {
    useSuspendVararg(f1, f2, f3)
}
