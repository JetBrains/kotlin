open class Base {
    fun foo(): String {
        return when (this) {
            is Derived -> baz()
            else -> "fail 1"
        }
    }

    private fun baz(): String = "OK"
}

class Derived : Base()

fun box(): String {
    return Derived().foo()
}
