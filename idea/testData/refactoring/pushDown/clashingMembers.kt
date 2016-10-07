abstract class <caret>A {
    // INFO: {"checked": "true"}
    open val x: Int = 1
    // INFO: {"checked": "true"}
    abstract val y: Boolean
    // INFO: {"checked": "true", "toAbstract": "true"}
    open val z: Int = 1
    // INFO: {"checked": "true"}
    open fun foo(n: Int) = n + 1
    // INFO: {"checked": "true"}
    abstract fun foo(n: Int, m: Int)
    // INFO: {"checked": "true", "toAbstract": "true"}
    open fun foo(b: Boolean) = !b
    // INFO: {"checked": "true"}
    class X
}

class B : A {
    override val x: Int = 2

    override val y: Boolean get() = x > 0

    override val z: Int = 3

    override fun foo(n: Int) = n + 2

    override fun foo(n: Int, m: Int) = n + m

    override fun foo(b: Boolean) = true

    class X
}