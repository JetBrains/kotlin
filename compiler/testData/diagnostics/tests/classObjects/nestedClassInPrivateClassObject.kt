class A {
    private default object {
        class B {
            class C {
                default object {
                    fun foo() {}
                }
            }
        }
    }
}

fun f1() = A.Default.B.<!INVISIBLE_MEMBER!>C<!>

fun f2() = A.Default.B.C.<!INVISIBLE_MEMBER!>foo<!>()