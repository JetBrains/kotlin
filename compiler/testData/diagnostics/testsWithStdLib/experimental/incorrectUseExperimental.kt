// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

annotation class NotAMarker

<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn<!>
fun f1() {}

<!OPT_IN_ARGUMENT_IS_NOT_MARKER!>@OptIn(NotAMarker::class)<!>
fun f2() {}
