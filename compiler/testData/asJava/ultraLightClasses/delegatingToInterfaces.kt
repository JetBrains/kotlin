
interface Base {
    fun printMessage()
    fun printMessageLine()
}

class BaseImpl(val x: Int) : Base {
    override fun printMessage() { print(x) }
    override fun printMessageLine() { println(x) }
}

/** should load cls */
class Derived(b: Base) : Base by b {
    override fun printMessage() { print("abc") }
}
