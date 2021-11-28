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

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>f1<!>() = A.Companion.B.<!INVISIBLE_REFERENCE!>C<!>

fun f2() = A.Companion.B.<!INVISIBLE_REFERENCE!>C<!>.<!INVISIBLE_REFERENCE!>foo<!>()
