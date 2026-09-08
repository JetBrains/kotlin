@file:OptIn(ExperimentalVersionOverloading::class)

fun equivalentVersionStrings(
    @IntroducedAt("1") first: Int = 1,
    @IntroducedAt("01") second: Int = 2,
    @IntroducedAt("2") third: Int = 3,
): Int = first + second + third

fun box(): String {
    if (equivalentVersionStrings() != 6) return "FAIL default"
    if (equivalentVersionStrings(10, 20, 30) != 60) return "FAIL explicit"
    return "OK"
}
