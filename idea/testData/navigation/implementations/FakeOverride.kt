package testing

open class C {
    open fun <caret>f() {

    }
}

class A : C()

class B : C() {
    override fun f() {
    }
}

// REF: (in testing.B).f()
