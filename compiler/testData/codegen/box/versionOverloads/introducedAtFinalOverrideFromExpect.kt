// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect open class ExpectBase {
    open fun render(prefix: String = "O", suffix: String = "K"): String
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual open class ExpectBase {
    actual open fun render(prefix: String, suffix: String): String = prefix + suffix
}

@OptIn(ExperimentalVersionOverloading::class)
class FinalExpectOverride : ExpectBase() {
    override fun render(prefix: String, @IntroducedAt("1") suffix: String): String = prefix + suffix
}

fun box(): String {
    val expectBase: ExpectBase = FinalExpectOverride()
    if (expectBase.render("O") != "OK") return "FAIL base call"
    if (FinalExpectOverride().render("O") != "OK") return "FAIL final call"
    return "OK"
}
