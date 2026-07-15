package test

interface I {
    context(xx: Int)
    fun foo()
}

class Impl : I {
    context(xx: Int)
    override fun foo() {}
}

class Delegating(impl: I) : I by impl

// context_parameter: xx: function: test/Delegating.foo
