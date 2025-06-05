// TARGET_BACKEND: JVM

// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.IntroducedAt

data class C (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")
data class D (
    val a : Int = 1,
    @IntroducedAt("2") val a1: Int = 2,
    @IntroducedAt("1") val b: String = "3",
)