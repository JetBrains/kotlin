// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

@file:OptIn(ExperimentalVersionOverloading::class)

expect class ExpectedVersionedMember {
    constructor()

    fun render(
        value: String = "O",
        @IntroducedAt("1") suffix: String = "K",
    ): String
}

fun commonExpectedMember(): String = ExpectedVersionedMember().render()

// MODULE: platform()()(common)
// FILE: platform.kt

@file:OptIn(ExperimentalVersionOverloading::class)

actual class ExpectedVersionedMember actual constructor() {
    actual fun render(
        value: String,
        @IntroducedAt("1") suffix: String,
    ): String = value + suffix
}

fun box(): String {
    if (commonExpectedMember() != "OK") return "FAIL common member"
    if (ExpectedVersionedMember().render("A", "B") != "AB") return "FAIL explicit member"
    return "OK"
}
