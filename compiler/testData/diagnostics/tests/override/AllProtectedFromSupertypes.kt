package test

trait A {
    protected val a: String
}

open class C {
    protected val a: String = ""
}

class Subject : C(), A {
    val c = a
}