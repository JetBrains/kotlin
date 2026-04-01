// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalVersionOverloading::class)

@JvmName("javaName")
fun kotlinName(
    a : Int = 1,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) = "$a/$b/$c"
