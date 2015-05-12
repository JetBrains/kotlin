package test

class E1: Exception()

interface Base<T> {
    throws(E1::class)
    fun one(t: T) {}
}

class Derived: Base<String>