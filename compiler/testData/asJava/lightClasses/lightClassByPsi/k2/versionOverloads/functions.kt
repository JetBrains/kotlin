@file:OptIn(ExperimentalVersionOverloading::class)

class SimpleClass {
    fun Int.foo(
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$this/$b/$c"

    suspend fun bar(
        a : Int = 1,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$this/$a/$b/$c"
}
