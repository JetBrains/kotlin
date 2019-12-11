// !API_VERSION: 1.0

@SinceKotlin("1.1")
annotation class Anno1(val s: String)

annotation class Anno2 @SinceKotlin("1.1") constructor()


@Anno1("")
@Anno2
fun t1() {}
