// SKIP_TXT
// ISSUE: KT-52543

abstract class A {
    fun foo(a: Any) {
        if (a is A) {
            <!DEBUG_INFO_SMARTCAST!>a<!>.prv()
            if (a is B) {
                <!DEBUG_INFO_SMARTCAST!>a<!>.prv()
            }
        }

        if (a is B) {
            <!DEBUG_INFO_SMARTCAST!>a<!>.<!INVISIBLE_MEMBER!>prv<!>()
            if (<!USELESS_IS_CHECK!>a is A<!>) {
                <!DEBUG_INFO_SMARTCAST!>a<!>.<!INVISIBLE_MEMBER!>prv<!>()
            }
        }
    }

    private fun prv() {}
}


open class B : A()
