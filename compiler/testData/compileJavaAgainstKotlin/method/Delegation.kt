package test

interface Trait {
    fun foo()
    val bar: Int
}

class Impl: Trait {
    override fun foo() {}
    override val bar = 1
}

class Test : Trait by Impl()