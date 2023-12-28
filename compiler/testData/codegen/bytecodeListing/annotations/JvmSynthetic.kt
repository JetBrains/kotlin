// WITH_STDLIB

class Example {
    @JvmSynthetic
    val prop: String = "ABC"

    var prop2 = 5
        @JvmSynthetic public get
        @JvmSynthetic public set

    @field:JvmSynthetic
    val useSite = 0

    @get:JvmSynthetic @set:JvmSynthetic
    var useSite2 = 0

    @JvmSynthetic
    fun job() {}
}