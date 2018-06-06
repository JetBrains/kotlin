// !USE_EXPERIMENTAL: kotlin.Experimental

annotation class NotAMarker

<!USE_EXPERIMENTAL_WITHOUT_ARGUMENTS!>@UseExperimental<!>
fun f1() {}

<!USE_EXPERIMENTAL_ARGUMENT_IS_NOT_MARKER!>@UseExperimental(NotAMarker::class)<!>
fun f2() {}
