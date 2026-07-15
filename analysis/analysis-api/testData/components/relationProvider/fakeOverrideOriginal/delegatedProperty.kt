package test

interface I {
    val foo: Int
}

class Impl : I {
    override val foo: Int get() = 0
}

class Delegating(impl: I) : I by impl

// property: test/Delegating.foo
