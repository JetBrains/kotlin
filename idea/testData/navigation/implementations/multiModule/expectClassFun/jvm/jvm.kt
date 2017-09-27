package test

actual open class ExpectedChild : SimpleParent() {
    actual override fun foo(n: Int) {}
}

class ExpectedChildChildJvm : ExpectedChild() {
    override fun foo(n: Int) {}
}