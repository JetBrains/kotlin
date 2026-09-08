// FILE: defaults.kt

@file:OptIn(ExperimentalVersionOverloading::class)

private fun filePrivateDefault(): String = "K"

fun fileFacadeVersioned(
    value: String = "O",
    @IntroducedAt("1") suffix: String = filePrivateDefault(),
): String = value + suffix

private fun filePrivateIdentity(value: Int): Int = value

internal inline fun fileInlineVersioned(
    value: Int,
    @IntroducedAt("1") fallback: Int = filePrivateIdentity(value),
): Int = fallback

// FILE: main.kt

fun box(): String {
    if (fileFacadeVersioned() != "OK") return "FAIL facade default"
    if (fileFacadeVersioned("A") != "AK") return "FAIL facade partial"
    if (fileFacadeVersioned("A", "B") != "AB") return "FAIL facade full"
    if (fileInlineVersioned(42) != 42) return "FAIL inline default"
    if (fileInlineVersioned(42, 7) != 7) return "FAIL inline full"
    return "OK"
}
