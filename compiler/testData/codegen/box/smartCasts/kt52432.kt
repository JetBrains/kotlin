abstract class Base {
    fun foo(): String {
        return when (this) {
            is Derived -> baz()
            else -> "fail 1"
        }
    }

    abstract fun bar(): String

    private fun Derived.baz(): String = bar() + k
}

class Derived : Base() {
    override fun bar(): String = "O"
    val k: String get() = "K"
}

fun box(): String {
    return Derived().foo()
}
