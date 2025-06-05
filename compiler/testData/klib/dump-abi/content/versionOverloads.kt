// MODULE: versionOverloads_library

@file:OptIn(ExperimentalVersionOverloading::class)

open class A(
    val a: Int = 1,
    @IntroducedAt("1") val b: String = "A1",
    @IntroducedAt("2") val c: Float = 3f,
)

class B : A {
    constructor(a: Int, @IntroducedAt("1") b: String = "B1") : super(a, b)
    constructor(@IntroducedAt("1") b: String = "B2") : super(2, b)
    constructor(b: Boolean) : super(3)
}

data class C (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("1") private val b1: String = "",
    @IntroducedAt("2") val c: Float = 3f,
) {
    fun Int.extensionFn(
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$this/$b/$c"

    suspend fun suspendFn(
        a : Int = 1,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$this/$b/$c"

    fun trailingLambda(
        a : Int = 1,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
        f : (String) -> String
    ) = f("$a/$b/$c")
}