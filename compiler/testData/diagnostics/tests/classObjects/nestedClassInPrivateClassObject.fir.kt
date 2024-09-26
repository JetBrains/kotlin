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

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>f1<!>() = A.<!INVISIBLE_REFERENCE!>Companion<!>.B.C

fun f2() = A.<!INVISIBLE_REFERENCE!>Companion<!>.B.C.<!INVISIBLE_REFERENCE!>foo<!>()
