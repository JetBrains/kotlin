package test

interface X {
    fun foo()
}

interface Y {
    fun foo()
}

class <caret>C(delegate: X) : X by delegate, Y
