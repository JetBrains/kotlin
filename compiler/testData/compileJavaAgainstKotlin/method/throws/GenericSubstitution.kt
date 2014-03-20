package test

class E1: Exception()

trait Base<T> {
    throws(javaClass<E1>())
    fun one(t: T) {}
}

class Derived: Base<String>