@file:OptIn(ExperimentalVersionOverloading::class)

enum class EnumWithMultipleVersions(
    val first: String = "O",
    @IntroducedAt("1") val second: String = "K",
    @IntroducedAt("2") val third: String = "!",
) {
    DEFAULT,
    PARTIAL("P", "Q"),
    FULL("A", "B", "C");
}

fun box(): String {
    if (EnumWithMultipleVersions.DEFAULT.first != "O" ||
        EnumWithMultipleVersions.DEFAULT.second != "K" ||
        EnumWithMultipleVersions.DEFAULT.third != "!"
    ) return "FAIL default"
    if (EnumWithMultipleVersions.PARTIAL.first != "P" ||
        EnumWithMultipleVersions.PARTIAL.second != "Q" ||
        EnumWithMultipleVersions.PARTIAL.third != "!"
    ) return "FAIL partial"
    if (EnumWithMultipleVersions.FULL.first != "A" ||
        EnumWithMultipleVersions.FULL.second != "B" ||
        EnumWithMultipleVersions.FULL.third != "C"
    ) return "FAIL full"
    if (EnumWithMultipleVersions.values().size != 3) return "FAIL entries"
    return "OK"
}
