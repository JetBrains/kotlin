// TARGET_BACKEND: JVM

// WITH_STDLIB

data class C (
    val a : Int = 1,
    @kotlin.jvm.IntroducedAt("1") val b: String = "",
    @kotlin.jvm.IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")
data class D (
    val a : Int = 1,
    @kotlin.jvm.IntroducedAt("2") val a1: Int = 2,
    @kotlin.jvm.IntroducedAt("1") val b: String = "3",
)