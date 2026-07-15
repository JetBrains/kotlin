package test

interface I {
    fun foo()
}

class Impl : I {
    override fun foo() {}
}

class Delegating(impl: I) : I by impl

// function: test/Delegating.foo
