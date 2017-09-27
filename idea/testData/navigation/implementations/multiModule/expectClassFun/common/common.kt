package test

open class SimpleParent {
    open fun foo(n: Int) {}
}

expect open class ExpectedChild : SimpleParent {
    override fun <caret>foo(n: Int)
}

class ExpectedChildChild : ExpectedChild() {
    override fun foo(n: Int) {}
}

class SimpleChild : SimpleParent() {
    override fun foo(n: Int) {}
}

// REF: [common] (in test.ExpectedChildChild).foo(Int)
// REF: [jvm] (in test.ExpectedChildChildJvm).foo(Int)