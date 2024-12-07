// WITH_STDLIB
fun container() {
    fun() = 1
    fun() = "Hello"
    fun() = true
    fun() = 1.0
    fun() = 1.0f
    fun() = 'A'
    fun() = 1L
    fun() = 1u
    fun() = 1UL
    fun() = 0xFFFF_FFFF_FFFFu
    fun() {}

    fun(): Int? = 1
    fun(): String? = "Hello"
    fun(): Boolean? = true
    fun(): Double? = 1.0
    fun(): Float? = 1.0f
    fun(): Char? = 'A'
    fun(): Long? = 1L
    fun(): UInt? = 1u
    fun(): ULong? = 1UL
    fun(): ULong? = 0xFFFF_FFFF_FFFFu
    fun(): Unit {}
}
