// TARGET_BACKEND: JVM

// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.experimental.IntroducedAt
import kotlin.jvm.JvmOverloads

class X {
    @Suppress("CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION")
    @JvmOverloads
    fun foo(
        a : Int,
        @IntroducedAt("1") B: String = "",
        @IntroducedAt("1") b1: String = "",
        @IntroducedAt("2") c: Float = 0f,
        @IntroducedAt("3") d: Int = 0,
    ) {}

}

@Suppress("CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
@JvmOverloads
fun bar(
    a: Int = 1,
    @IntroducedAt("3") a1: Int = 2,
    @IntroducedAt("2") b : Int = 3,
    @IntroducedAt("4") c: String = ""
) {}