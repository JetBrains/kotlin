package test

interface X {
    fun foo()
    val bar: Int
}

interface Y {
    fun foo()
    val bar: Int
}

interface Z : X, Y

class <caret>C(delegate: Z) : Z by delegate
