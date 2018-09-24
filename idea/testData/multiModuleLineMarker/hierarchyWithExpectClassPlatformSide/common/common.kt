// !CHECK_HIGHLIGHTING

package test

open class SimpleParent {
    open fun foo(n: Int) {}
    open val bar: Int get() = 1
}

expect open class ExpectedChild : SimpleParent {
    override fun foo(n: Int)
    override val bar: Int
}

class ExpectedChildChild : ExpectedChild() {
    override fun foo(n: Int) {}
    override val bar: Int get() = 1
}

class SimpleChild : SimpleParent() {
    override fun foo(n: Int) {}
    override val bar: Int get() = 1
}