package test

actual open class ExpectedChild : SimpleParent() {
    actual override val bar: Int get() = 1
}

class ExpectedChildChildJvm : ExpectedChild() {
    override val bar: Int get() = 1
}