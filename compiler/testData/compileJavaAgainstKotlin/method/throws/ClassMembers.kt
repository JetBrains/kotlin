package test

class E1: Exception()
class E2: Exception()

class Test {
    throws()
    fun none() {}

    throws(E1::class)
    fun one() {}

    throws(E1::class, E2::class)
    fun two() {}
}