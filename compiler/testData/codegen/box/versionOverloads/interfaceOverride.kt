@file:OptIn(ExperimentalVersionOverloading::class)

interface VersionedRenderer {
    fun render(prefix: String = "O", suffix: String = "K"): String
}

class FinalInterfaceRenderer : VersionedRenderer {
    override fun render(prefix: String, @IntroducedAt("1") suffix: String): String = prefix + suffix
}

fun box(): String {
    val renderer: VersionedRenderer = FinalInterfaceRenderer()
    if (renderer.render("O") != "OK") return "FAIL interface call"
    if (FinalInterfaceRenderer().render("O") != "OK") return "FAIL final call"
    return "OK"
}
