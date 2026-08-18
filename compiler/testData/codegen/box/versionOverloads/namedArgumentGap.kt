@file:OptIn(ExperimentalVersionOverloading::class)

fun namedArgumentGap(
    a: Int,
    @IntroducedAt("1") b: String = "b",
    @IntroducedAt("2") c: String = "c",
): String = "$a$b$c"

fun box(): String {
    if (namedArgumentGap(a = 1, c = "x") != "1bx") return "FAIL omitted middle argument"
    if (namedArgumentGap(a = 1, b = "y", c = "x") != "1yx") return "FAIL full named arguments"
    return "OK"
}
