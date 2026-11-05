package test

interface I {
    fun foo(text: String)
    val bar: Int
    var baz: String
}

class <caret>C(delegate: I) : I by delegate
