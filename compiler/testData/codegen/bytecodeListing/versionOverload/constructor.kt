// TARGET_BACKEND: JVM

// WITH_STDLIB

class C (
    val a : Int = 1,
    @kotlin.jvm.IntroducedAt("1") val b: String = "",
    @kotlin.jvm.IntroducedAt("1") private val b1: String = "",
    @kotlin.jvm.IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")
class D (
    val a : Int = 1,
    @kotlin.jvm.IntroducedAt("2") val a1: String = "",
    @kotlin.jvm.IntroducedAt("1") private val b: String = "",
    @kotlin.jvm.IntroducedAt("1") val c: Float = 3f,
)