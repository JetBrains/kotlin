// LIBRARY_PLATFORMS: JVM

@JvmName("javaName")
fun kotlinName(
    a : Int = 1,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) = "$a/$b/$c"

// LIGHT_ELEMENTS_NO_DECLARATION: JvmNameKt.class[javaName;javaName]
