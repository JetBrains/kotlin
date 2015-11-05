class A {
    private companion object {
        class B {
            class C {
                companion object {
                    fun foo() {}
                }
            }
        }
    }
}

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>f1<!>() = A.<!INVISIBLE_REFERENCE!>Companion<!>.<!INVISIBLE_REFERENCE!>B<!>.<!INVISIBLE_MEMBER!>C<!>

fun f2() = A.<!INVISIBLE_REFERENCE!>Companion<!>.<!INVISIBLE_REFERENCE!>B<!>.<!INVISIBLE_REFERENCE!>C<!>.<!INVISIBLE_MEMBER!>foo<!>()