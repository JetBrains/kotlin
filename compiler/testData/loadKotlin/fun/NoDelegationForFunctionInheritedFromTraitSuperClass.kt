package test

abstract class A {
    abstract fun foo()
}

trait X : A {
    fun bar() {
    }
}

open class B() : A() {
    override fun foo() {
    }
}

class C() : A(), X {
    override fun foo() {
    }
}

class D(val c: C) : B(), X by c {
}