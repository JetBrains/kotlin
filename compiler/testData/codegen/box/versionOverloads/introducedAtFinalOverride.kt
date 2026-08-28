@file:OptIn(ExperimentalVersionOverloading::class)

open class OverrideBase {
    open fun render(prefix: String = "O", suffix: String = "K"): String = prefix + suffix
}

class FinalOverride : OverrideBase() {
    override fun render(prefix: String, @IntroducedAt("1") suffix: String): String = prefix + suffix
}

fun box(): String {
    val overrideBase: OverrideBase = FinalOverride()
    if (overrideBase.render("O") != "OK") return "FAIL base call"
    if (FinalOverride().render("O") != "OK") return "FAIL final call"
    return "OK"
}
