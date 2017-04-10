package test

class E1: Exception()

interface Base<T> {
    @Throws(E1::class)
    fun one(t: T) {}
}

class Derived: Base<String>