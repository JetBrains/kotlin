interface SomeInterface {
    fun foo(x: Int, y: String): String
    val bar: Boolean
}
class SomeClass : SomeInterface {
    private val baz = 42
    override fun foo(x: Int, y: String): String {
        return y + x + baz
    }
    override var bar: Boolean
        get() = true
        set(value) {}
    lateinit var fau: Double
}
inline class InlineClass