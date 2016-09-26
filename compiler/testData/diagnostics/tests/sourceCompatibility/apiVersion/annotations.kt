// !API_VERSION: 1.0

@SinceKotlin("1.1")
annotation class Anno1(val s: String)

annotation class Anno2 @SinceKotlin("1.1") constructor()


@<!UNRESOLVED_REFERENCE, API_NOT_AVAILABLE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>Anno1<!>("")
@<!UNRESOLVED_REFERENCE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>Anno2<!>
fun t1() {}
