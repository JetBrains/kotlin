package test

interface Base<T> {
    fun get(): T
    var value: T
}

class <caret>C(delegate: Base<Int>) : Base<Int> by delegate
