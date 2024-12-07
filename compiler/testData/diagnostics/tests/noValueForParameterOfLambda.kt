// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_IDENTICAL

fun test1(lambda: Int.(String) -> Unit) {
    lambda(<!NO_VALUE_FOR_PARAMETER!>1)<!>
}

fun test2(lambda: Int.(s: String) -> Unit) {
    lambda(<!NO_VALUE_FOR_PARAMETER!>1)<!>
}

fun test3(lambda: Int.(@ParameterName("x") String) -> Unit) {
    lambda(<!NO_VALUE_FOR_PARAMETER!>1)<!>
}
