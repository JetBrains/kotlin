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

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>f1<!>() = A.Companion.B.<!INVISIBLE_MEMBER!>C<!>

fun f2() = A.Companion.B.C.<!INVISIBLE_MEMBER!>foo<!>()