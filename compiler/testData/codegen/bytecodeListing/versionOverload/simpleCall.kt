// TARGET_BACKEND: JVM

// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.experimental.IntroducedAt

class X {
    fun foo(
        a : Int,
        @IntroducedAt("1") B: String = "",
        @IntroducedAt("1") b1: String = "",
        @IntroducedAt("2") c: Float = 0f,
    ) {}

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun mid(
        a : Int,
        @IntroducedAt("2") a1: Int = 1,
        @IntroducedAt("1") b: String = "",
        @IntroducedAt("1") c: Float = 0f,
    ) {}
}

fun foo2(
    a : Int,
    @IntroducedAt("1") B: String = "",
    @IntroducedAt("1") b1: String = "",
    @IntroducedAt("2") c: Float = 0f,
    f: () -> Unit
) {}

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")
fun mid2(
    a : Int,
    @IntroducedAt("2") a1: Int = 1,

    @IntroducedAt("1") b: String = "",
    @IntroducedAt("1") c: Float = 0f,
    f: () -> Unit
) {}