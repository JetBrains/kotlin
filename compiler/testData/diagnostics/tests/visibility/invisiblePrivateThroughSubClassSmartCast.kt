// FIR_IDENTICAL
// SKIP_TXT

abstract class A {
    fun foo(a: A) {
        a.prv()
        if (a is B) {
            a.prv()
        }
    }

    private fun prv() {}
}

abstract class B : A()
