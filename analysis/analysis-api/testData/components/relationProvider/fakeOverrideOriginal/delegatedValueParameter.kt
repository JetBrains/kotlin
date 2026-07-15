package test

interface I {
    fun foo(xx: Int)
}

class Impl : I {
    override fun foo(xx: Int) {}
}

class Delegating(impl: I) : I by impl

// value_parameter: xx: function: test/Delegating.foo
