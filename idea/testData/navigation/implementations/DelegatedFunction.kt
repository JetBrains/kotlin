package testing

interface I {
    fun <caret>f() {

    }
}

class A(i: I) : I by i

class B(i: I) : I by i {
    override fun f() {
    }
}

class C(i: I) : I by i

// REF: (in testing.B).f()

