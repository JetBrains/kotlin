package test

class E1: Exception()
class E2: Exception()

trait Trait {
    throws()
    fun none()

    throws(E1::class)
    fun one()

    throws(E1::class, E2::class)
    fun two()
}

class Impl: Trait {
    override fun none() {}
    override fun one() {}
    override fun two() {}
}

class Test: Trait by Impl()