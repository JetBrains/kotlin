// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

annotation class NotAMarker

annotation class NotBMarker

<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn<!>
fun f1() {}

@OptIn(<!OPT_IN_ARGUMENT_IS_NOT_MARKER!>NotAMarker::class<!>)
fun f2() {}

@OptIn(<!OPT_IN_ARGUMENT_IS_NOT_MARKER!>NotAMarker::class<!>, <!OPT_IN_ARGUMENT_IS_NOT_MARKER!>NotBMarker::class<!>)
fun f3() {}

