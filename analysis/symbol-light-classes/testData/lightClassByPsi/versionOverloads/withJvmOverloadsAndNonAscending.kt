// LIBRARY_PLATFORMS: JVM

@file:OptIn(ExperimentalVersionOverloading::class)
@file:Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")

@JvmOverloads
fun nonAscendingSameType(
    a: Int = 1,
    @IntroducedAt("3") b: Int = 2,
    @IntroducedAt("4") c: Int = 3,
) {
}

@JvmOverloads
fun emptyBase(
    @IntroducedAt("1") b: String = "hello",
    a: Int = 1,
) {
}

@JvmOverloads
fun nonAscending(
    a: Int = 1,
    @IntroducedAt("3") a1: String = "hello",
    @IntroducedAt("2") b: Boolean = true,
) {

}

@JvmOverloads
fun randomSameType(
    a: Int = 1,
    @IntroducedAt("3") a1: Int = 3,
    @IntroducedAt("2") b: Int = 2,
    @IntroducedAt("4") c: Int = 4,
) {
}

@JvmOverloads
fun randomAlmostSameType(
    @IntroducedAt("1.0-beta.1") a1: Int = 3,
    a: Int = 1,
    @IntroducedAt("1.0") c: String = "",
    @IntroducedAt("1.0-alpha.2") b: Int = 2,
) {
}

@JvmOverloads
fun random(
    a: Int = 1,
    @IntroducedAt("3") a1: Boolean = false,
    @IntroducedAt("2") b: String = "2",
    @IntroducedAt("4") c: Long = 1L,
) {
}
