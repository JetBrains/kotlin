// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

annotation class NotAMarker

<!USE_EXPERIMENTAL_WITHOUT_ARGUMENTS!>@OptIn<!>
fun f1() {}

<!USE_EXPERIMENTAL_ARGUMENT_IS_NOT_MARKER!>@OptIn(NotAMarker::class)<!>
fun f2() {}
