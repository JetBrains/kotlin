// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

class IntroducedOnly @JvmExposeBoxed constructor(
    @IntroducedAt("3") val value: UInt = 1u,
)

class IntroducedAfterBase @JvmExposeBoxed constructor(
    val text: String = "text",
    @IntroducedAt("3") val value: UInt = 2u,
)

// LIGHT_ELEMENTS_NO_DECLARATION: IntroducedAfterBase.class[getValue-pVg5ArA], IntroducedOnly.class[getValue-pVg5ArA]
