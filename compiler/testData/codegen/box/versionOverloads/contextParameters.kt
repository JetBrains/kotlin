// LANGUAGE: +ContextParameters

@file:OptIn(ExperimentalVersionOverloading::class)

context(prefix: String)
fun contextVersioned(
    value: String = "O",
    @IntroducedAt("1") suffix: String = "K",
): String = prefix + value + suffix

fun box(): String {
    if (with("P") { contextVersioned() } != "POK") return "FAIL default"
    if (with("P") { contextVersioned("A", "B") } != "PAB") return "FAIL explicit"
    return "OK"
}
