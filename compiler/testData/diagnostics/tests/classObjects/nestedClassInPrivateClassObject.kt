class A {
    private class object {
        class B {
            class C {
                class object {
                    fun foo() {}
                }
            }
        }
    }
}

fun f1() = A.B.<!INVISIBLE_MEMBER!>C<!>

fun f2() = A.B.C.<!INVISIBLE_MEMBER!>foo<!>()