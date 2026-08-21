@file:OptIn(ExperimentalVersionOverloading::class)

fun trailingVersioned(
    value: String = "O",
    @IntroducedAt("1") suffix: String = "K",
    @IntroducedAt("2") punctuation: String = "!",
    transform: (String) -> String,
): String = transform(value + suffix + punctuation)

fun box(): String {
    if (trailingVersioned { it } != "OK!") return "FAIL default"
    if (trailingVersioned("A", "B") { it } != "AB!") return "FAIL second"
    if (trailingVersioned("A", "B", "C") { it + "?" } != "ABC?") return "FAIL all"
    return "OK"
}
