@file:OptIn(ExperimentalVersionOverloading::class)

enum class VersionedEnum(
    val prefix: String,
    @IntroducedAt("1") val suffix: String = "K",
) {
    OK("O"),
    VALUE("V", "!");

    fun render(@IntroducedAt("1") extraSuffix: String = suffix): String = prefix + extraSuffix
}

fun box(): String {
    if (VersionedEnum.OK.prefix + VersionedEnum.OK.suffix != "OK") return "FAIL default"
    if (VersionedEnum.VALUE.prefix + VersionedEnum.VALUE.suffix != "V!") return "FAIL explicit"
    if (VersionedEnum.OK.render() != "OK") return "FAIL member default"
    if (VersionedEnum.OK.render("!") != "O!") return "FAIL member explicit"
    if (VersionedEnum.values().size != 2) return "FAIL entries"
    return "OK"
}
