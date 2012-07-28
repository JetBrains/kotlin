package test

trait A {
    protected val a: String
}

trait B {
    protected val a: String
}

open class C {
    private val a: String = ""
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>Subject<!> : C(), A, B {
    val c = a
}