@file:OptIn(ExperimentalVersionOverloading::class)

object VersionedObject {
    fun render(
        value: String = "O",
        @IntroducedAt("1") suffix: String = "K",
    ): String = value + suffix

    object Nested {
        fun render(
            value: String = "N",
            @IntroducedAt("1") suffix: String = "!",
        ): String = value + suffix
    }
}

fun box(): String {
    if (VersionedObject.render() != "OK") return "FAIL object default"
    if (VersionedObject.render("A") != "AK") return "FAIL object partial"
    if (VersionedObject.Nested.render() != "N!") return "FAIL nested default"
    if (VersionedObject.Nested.render("A") != "A!") return "FAIL nested partial"
    return "OK"
}
