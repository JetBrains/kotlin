// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

@file:OptIn(ExperimentalVersionOverloading::class)

expect fun expectedVersioned(
    value: String = "O",
    @IntroducedAt("1") suffix: String = "K",
): String

expect class ExpectedVersionedBox {
    constructor(
        value: String = "O",
        @IntroducedAt("1") suffix: String = "K",
    )

    val result: String
}

fun commonExpectedDefaults(): String = expectedVersioned() + ExpectedVersionedBox().result

// MODULE: platform()()(common)
// FILE: platform.kt

@file:OptIn(ExperimentalVersionOverloading::class)

actual fun expectedVersioned(
    value: String,
    @IntroducedAt("1") suffix: String,
): String = value + suffix

actual class ExpectedVersionedBox actual constructor(
    private val value: String,
    @IntroducedAt("1") private val suffix: String,
) {
    actual val result: String = value + suffix
}

fun box(): String {
    if (commonExpectedDefaults() != "OKOK") return "FAIL common defaults"
    if (expectedVersioned("A", "B") != "AB") return "FAIL explicit function"
    if (ExpectedVersionedBox("A", "B").result != "AB") return "FAIL explicit constructor"
    return "OK"
}
