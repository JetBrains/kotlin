// SKIP_TXT
// ISSUE: KT-52543

abstract class A {
    fun foo(a: Any) {
        if (a is A) {
            a.prv()
            if (a is B) {
                a.<!INVISIBLE_REFERENCE!>prv<!>()
            }
        }

        if (a is B) {
            a.<!INVISIBLE_REFERENCE!>prv<!>()
            if (<!USELESS_IS_CHECK!>a is A<!>) {
                a.<!INVISIBLE_REFERENCE!>prv<!>()
            }
        }
    }

    private fun prv() {}
}


open class B : A()
