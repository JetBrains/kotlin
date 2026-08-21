@file:OptIn(ExperimentalVersionOverloading::class)

fun topLevelFun(a: Int, @IntroducedAt("1") b: String = "b", @IntroducedAt("2") c: String = "c") = a.toString() + b + c

class WithVersionedConstructor(
    val prefix: String,
    @IntroducedAt("1") val suffix: String = "Suffix",
) {
    fun memberFun(number: Int, @IntroducedAt("1") extraSuffix: String = "Extra"): String =
        prefix + number + extraSuffix + suffix
    fun String.extensionFun(number: Int, @IntroducedAt("1") extraSuffix: String = "Extra"): String =
        prefix + this + number + extraSuffix + suffix
}

fun String.withIntroducedSuffix(@IntroducedAt("1") suffix: String = ""): String = this + suffix

fun box(): String {
    if (topLevelFun(1) != "1bc") return "fail1: ${topLevelFun(1)}"

    val withVersionedConstructor = WithVersionedConstructor("Prefix")
    if (withVersionedConstructor.suffix != "Suffix") return "fail3"
    if (withVersionedConstructor.memberFun(3) != "Prefix3ExtraSuffix") return "fail4"
    if (withVersionedConstructor.run { "Value".extensionFun(3) } != "PrefixValue3ExtraSuffix") return "fail5"

    if ("OK".withIntroducedSuffix() != "OK") return "fail17"

    return "OK"
}
