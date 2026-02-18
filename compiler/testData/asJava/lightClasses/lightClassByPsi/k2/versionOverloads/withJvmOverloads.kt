// LIBRARY_PLATFORMS: JVM

@file:OptIn(ExperimentalVersionOverloading::class)
@file:Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION")

@JvmOverloads
fun sameType(
    a: Int = 1,
    @IntroducedAt("1") b: Int = 2,
    @IntroducedAt("2") c: Int = 3,
) {
}

@JvmOverloads
fun emptyBase(
    a: Int = 1,
    @IntroducedAt("1") b: String = "hello",
) {
}

@JvmOverloads
fun ascending1(
    a: Int,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("1") b1: String = "bye",
) {
}

@JvmOverloads
fun ascending2(
    a: Int,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("1") b1: String = "bye",
    @IntroducedAt("2") c: Float = 0f,
) {
}

@JvmOverloads
fun ascending3(
    a: Int,
    @IntroducedAt("1.0-alpha.2") b: String = "hello",
    @IntroducedAt("1.0-alpha.2") b1: String = "bye",
    @IntroducedAt("1.0-beta.1") c: Float = 0f,
    @IntroducedAt("1.0") d: Int = 2,
) {
}
