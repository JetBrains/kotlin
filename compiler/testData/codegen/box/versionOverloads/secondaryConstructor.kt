@file:OptIn(ExperimentalVersionOverloading::class)

class SecondaryConstructor {
    val value: String

    constructor(
        prefix: String = "O",
        @IntroducedAt("1") suffix: String = "K",
        @IntroducedAt("2") punctuation: String = "!",
    ) {
        value = prefix + suffix + punctuation
    }

    constructor(flag: Boolean) : this("O", if (flag) "K" else "X")
}

fun box(): String {
    if (SecondaryConstructor().value != "OK!") return "FAIL default"
    if (SecondaryConstructor("A").value != "AK!") return "FAIL first"
    if (SecondaryConstructor("A", "B").value != "AB!") return "FAIL second"
    if (SecondaryConstructor("A", "B", "C").value != "ABC") return "FAIL all"
    if (SecondaryConstructor(true).value != "OK!") return "FAIL delegated"
    if (SecondaryConstructor(false).value != "OX!") return "FAIL delegated false"
    return "OK"
}
