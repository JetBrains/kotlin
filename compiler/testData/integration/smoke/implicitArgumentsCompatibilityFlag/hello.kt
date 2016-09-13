package Hello

open class Outer<X, Y> {
    inner class Inner<Z>

    val x: Inner<String> = Inner()
}

class Derived : Outer<String, Int>() {
    fun foo(): Inner<Char> = null!!
}
