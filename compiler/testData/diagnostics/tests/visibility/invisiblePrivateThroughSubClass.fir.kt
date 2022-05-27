// SKIP_TXT

abstract class A {
    fun foo(b: B) {
        b.<!INVISIBLE_REFERENCE!>prv<!>()
    }

    private fun prv() {}
}

abstract class B : A()
