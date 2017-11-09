package test

open class SimpleParent {
    open val bar: Int get() = 1
}

expect open class ExpectedChild : SimpleParent {
    override val <caret>bar: Int
}

class ExpectedChildChild : ExpectedChild() {
    override val bar: Int get() = 1
}

class SimpleChild : SimpleParent() {
    override val bar: Int get() = 1
}

// REF: [common] (in test.ExpectedChildChild).bar
// REF: [jvm] (in test.ExpectedChildChildJvm).bar