// ISSUE: KT-88612
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

fun varargBeforeVersioned(
    vararg values: String,
    @IntroducedAt("1") suffix: String = "!",
): String = values.joinToString("") + suffix

fun box(): String {
    if (varargBeforeVersioned() != "!") return "FAIL default"
    if (varargBeforeVersioned("O", "K") != "OK!") return "FAIL positional vararg"
    if (varargBeforeVersioned(*arrayOf("O", "K"), suffix = "!") != "OK!") return "FAIL spread vararg"
    if (varargBeforeVersioned("O", suffix = "K") != "OK") return "FAIL named suffix"
    return "OK"
}
