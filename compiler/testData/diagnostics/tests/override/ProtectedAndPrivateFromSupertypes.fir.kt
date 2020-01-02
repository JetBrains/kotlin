package test

interface A {
    val a: String
}

interface B {
    val a: String
}

open class C {
    private val a: String = ""
}

class Subject : C(), A, B {
    val c = a
}