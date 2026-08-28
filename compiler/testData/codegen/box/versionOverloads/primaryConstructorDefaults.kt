@file:OptIn(ExperimentalVersionOverloading::class)

private fun privateConstructorDefault(): String = "K"

class PrimaryVersionedConstructor(
    val prefix: String,
    @IntroducedAt("1") suffix: String = privateConstructorDefault(),
) {
    val value: String = prefix + suffix
}

fun box(): String {
    if (PrimaryVersionedConstructor("O").value != "OK") return "FAIL default"
    if (PrimaryVersionedConstructor("A", "B").value != "AB") return "FAIL explicit"
    return "OK"
}
