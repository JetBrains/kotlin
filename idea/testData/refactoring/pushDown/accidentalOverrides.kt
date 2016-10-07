abstract class <caret>A {
    // INFO: {"checked": "true"}
    abstract val y: Boolean
    // INFO: {"checked": "true", "toAbstract": "true"}
    val z: Int = 1
    // INFO: {"checked": "true"}
    abstract fun foo(n: Int, m: Int)
    // INFO: {"checked": "true", "toAbstract": "true"}
    fun foo(b: Boolean) = !b
}

class B : A {
    val x: Int = 2

    val y: Boolean get() = x > 0

    val z: Int = 3

    fun foo(n: Int) = n + 2

    fun foo(n: Int, m: Int) = n + m

    fun foo(b: Boolean) = true
}