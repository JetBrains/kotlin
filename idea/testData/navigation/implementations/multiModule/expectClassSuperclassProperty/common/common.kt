package test

open class SimpleParent {
    open val <caret>bar: Int get() = 1
}

expect open class ExpectedChild : SimpleParent {
    override val bar: Int
}

class ExpectedChildChild : ExpectedChild() {
    override val bar: Int get() = 1
}

class SimpleChild : SimpleParent() {
    override val bar: Int get() = 1
}

// REF: [common] (in test.ExpectedChild).bar
// REF: [common] (in test.ExpectedChildChild).bar
// REF: [common] (in test.SimpleChild).bar
// REF: [jvm] (in test.ExpectedChild).bar
// REF: [jvm] (in test.ExpectedChildChildJvm).bar