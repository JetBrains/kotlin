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

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>f1<!>() = A.Companion.B.C

fun f2() = A.Companion.B.C.foo()
