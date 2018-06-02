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

// REF: [testModule_Common] (in test.ExpectedChildChild).bar
// REF: [testModule_JVM] (in test.ExpectedChild).bar
// REF: [testModule_JVM] (in test.ExpectedChildChildJvm).bar