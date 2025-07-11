// TARGET_BACKEND: JVM

// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.experimental.IntroducedAt

class C (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("1") private val b1: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")
class D (
    val a : Int = 1,
    @IntroducedAt("2") val a1: String = "",
    @IntroducedAt("1") private val b: String = "",
    @IntroducedAt("1") val c: Float = 3f,
)