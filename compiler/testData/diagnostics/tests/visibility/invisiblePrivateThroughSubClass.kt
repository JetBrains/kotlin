// SKIP_TXT

abstract class A {
    fun foo(b: B) {
        b.<!INVISIBLE_MEMBER!>prv<!>()
    }

    private fun prv() {}
}

abstract class B : A()
