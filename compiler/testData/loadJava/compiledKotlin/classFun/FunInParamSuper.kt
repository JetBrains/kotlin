package test

open class Base<T>() {
    fun foo(): T = throw Exception()
}

class Inh() : Base<String>()
