// WITH_STDLIB
interface I {
    fun int(): Int?
    fun string(): String?
    fun boolean(): Boolean?
    fun double(): Double?
    fun float(): Float?
    fun char(): Char?
    fun long(): Long?
    fun uInt(): UInt?
    fun uLong(): ULong?
    fun unit(): Unit?
}

class C : I {
    override fun int() = 1
    override fun string() = "Hello"
    override fun boolean() = true
    override fun double() = 1.0
    override fun float() = 1.0f
    override fun char() = 'a'
    override fun long() = 1L
    override fun uInt() = 1u
    override fun uLong() = 1UL
    override fun unit() {}
}