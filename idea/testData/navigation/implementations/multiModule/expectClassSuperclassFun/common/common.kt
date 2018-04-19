package test

open class SimpleParent {
    open fun <caret>foo(n: Int) {}
}

expect open class ExpectedChild : SimpleParent {
    override fun foo(n: Int)
}

class ExpectedChildChild : ExpectedChild() {
    override fun foo(n: Int) {}
}

class SimpleChild : SimpleParent() {
    override fun foo(n: Int) {}
}

// REF: [testModule_Common] (in test.ExpectedChild).foo(Int)
// REF: [testModule_Common] (in test.ExpectedChildChild).foo(Int)
// REF: [testModule_Common] (in test.SimpleChild).foo(Int)
// REF: [testModule_JVM] (in test.ExpectedChild).foo(Int)
// REF: [testModule_JVM] (in test.ExpectedChildChildJvm).foo(Int)