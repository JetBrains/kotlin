@OptIn(ExperimentalVersionOverloading::class)
fun fo<caret>o(
    a: Int = 1,
    @IntroducedAt("1") b: Int = 2,
    @IntroducedAt("2") c: Int = 3,
) {}
