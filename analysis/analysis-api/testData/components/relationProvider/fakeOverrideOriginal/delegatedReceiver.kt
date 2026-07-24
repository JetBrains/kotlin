package test

interface I {
    fun Int.foo()
}

class Impl : I {
    override fun Int.foo() {}
}

class Delegating(impl: I) : I by impl

// receiver_parameter: function: test/Delegating.foo
