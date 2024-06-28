// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63853

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