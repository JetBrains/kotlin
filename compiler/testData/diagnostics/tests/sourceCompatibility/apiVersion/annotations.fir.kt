// !API_VERSION: 1.0

@SinceKotlin("1.1")
annotation class Anno1(val s: String)

annotation class Anno2 @SinceKotlin("1.1") constructor()


@<!API_NOT_AVAILABLE!>Anno1<!>("")
@<!UNRESOLVED_REFERENCE!>Anno2<!>
fun t1() {}
