open class Base {
    open fun foo(): Int = 1
}

class Derived: Base() {
    override fun foo(): Int = 2
    fun boo(): Int = 3
}

fun main() {
    val x = Base()
    "".toString()
    <expr>if (x !is Derived) return</expr>
    "".toString()
}