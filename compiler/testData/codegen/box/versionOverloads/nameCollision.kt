@file:OptIn(ExperimentalVersionOverloading::class)

fun collidingOverload(value: Int, @IntroducedAt("1") suffix: String = "K"): String =
    value.toString() + suffix

fun collidingOverload(value: String): String = value + "!"

fun box(): String {
    if (collidingOverload("1") != "1!") return "FAIL hand-written overload"
    if (collidingOverload(1) != "1K") return "FAIL generated wrapper"
    if (collidingOverload(1, "K") != "1K") return "FAIL versioned overload"
    return "OK"
}
