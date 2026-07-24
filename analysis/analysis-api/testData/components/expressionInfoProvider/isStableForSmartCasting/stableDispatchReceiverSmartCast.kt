open class Base {
    fun test() {
        if (this is Derived) {
            println(<expr>this</expr>.value)
        }
    }
}

class Derived : Base() {
    val value: Int = 0
}
