package test

interface A {
    fun foo(): Any
    fun bar()
}

interface B : A {
    override fun foo(): String
}

class <caret>C(delegate: B) : B by delegate
