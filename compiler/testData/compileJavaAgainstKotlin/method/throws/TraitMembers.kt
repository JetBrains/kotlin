package test

class E1: Exception()
class E2: Exception()

interface Trait {
    @Throws()
    fun none() {}

    @Throws(E1::class)
    fun one() {}

    @Throws(E1::class, E2::class)
    fun two() {}
}

class Test: Trait