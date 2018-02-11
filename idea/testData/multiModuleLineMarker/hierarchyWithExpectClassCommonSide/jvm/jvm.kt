// !CHECK_HIGHLIGHTING

package test

actual open class ExpectedChild : SimpleParent() {
    actual override fun foo(n: Int) {}
    actual override val bar: Int get() = 1
}

class ExpectedChildChildJvm : ExpectedChild() {
    override fun foo(n: Int) {}
    override val bar: Int get() = 1
}