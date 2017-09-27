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

// REF: [common] (in test.ExpectedChild).foo(Int)
// REF: [common] (in test.ExpectedChildChild).foo(Int)
// REF: [common] (in test.SimpleChild).foo(Int)
// REF: [jvm] (in test.ExpectedChild).foo(Int)
// REF: [jvm] (in test.ExpectedChildChildJvm).foo(Int)