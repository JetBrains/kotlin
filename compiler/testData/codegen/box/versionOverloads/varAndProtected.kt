@file:OptIn(ExperimentalVersionOverloading::class)

class VersionedMutable(
    var prefix: String,
    @IntroducedAt("1") var suffix: String = "K",
) {
    protected fun protectedRender(
        separator: String = "",
        @IntroducedAt("1") punctuation: String = "!",
    ): String = prefix + separator + suffix + punctuation

    fun render(): String = protectedRender()

    fun renderWith(separator: String): String = protectedRender(separator, "!")
}

fun box(): String {
    val mutable = VersionedMutable("O")
    if (mutable.render() != "OK!") return "FAIL constructor default"
    mutable.prefix = "A"
    mutable.suffix = "B"
    if (mutable.renderWith("/") != "A/B!") return "FAIL mutable properties"
    return "OK"
}
