@file:OptIn(ExperimentalVersionOverloading::class)

fun <T : Number, R : CharSequence> boundedDefaults(
    value: T,
    text: R,
    @IntroducedAt("1") fallback: R? = null,
    @IntroducedAt("2") suffix: String = "${fallback ?: text}:$value",
): String = "$value/${fallback ?: text}/$suffix"

fun <T : Any> nullableGenericDefault(
    value: T,
    @IntroducedAt("1") fallback: T? = null,
): String = "$value/${fallback ?: "null"}"

fun box(): String {
    if (boundedDefaults(1, "O") != "1/O/O:1") return "FAIL bounded default"
    if (boundedDefaults(1, "O", "K") != "1/K/K:1") return "FAIL bounded partial"
    if (boundedDefaults(1, "O", "K", "!") != "1/K/!") return "FAIL bounded full"
    if (nullableGenericDefault("O") != "O/null") return "FAIL nullable default"
    if (nullableGenericDefault("O", "K") != "O/K") return "FAIL nullable full"
    return "OK"
}
