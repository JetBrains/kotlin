class E1: Exception()
class E2: Exception()

trait Trait {
    throws()
    fun none()

    throws(javaClass<E1>())
    fun one()

    throws(javaClass<E1>(), javaClass<E2>())
    fun two()
}

class Impl: Trait {
    override fun none() {}
    override fun one() {}
    override fun two() {}
}

class Test: Trait by Impl()