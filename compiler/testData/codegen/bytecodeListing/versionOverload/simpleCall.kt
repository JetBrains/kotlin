// TARGET_BACKEND: JVM

// WITH_STDLIB

class X {
    fun foo(
        a : Int,
        @kotlin.jvm.IntroducedAt("1") B: String = "",
        @kotlin.jvm.IntroducedAt("1") b1: String = "",
        @kotlin.jvm.IntroducedAt("2") c: Float = 0f,
    ) {}

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun mid(
        a : Int,
        @kotlin.jvm.IntroducedAt("2") a1: Int = 1,
        @kotlin.jvm.IntroducedAt("1") b: String = "",
        @kotlin.jvm.IntroducedAt("1") c: Float = 0f,
    ) {}
}