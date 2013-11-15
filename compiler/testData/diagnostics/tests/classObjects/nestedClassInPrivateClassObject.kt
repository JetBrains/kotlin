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

fun f1() = <!INVISIBLE_MEMBER!>A<!>.B.C

fun f2() = <!INVISIBLE_MEMBER!>A<!>.B.C.<!INVISIBLE_MEMBER!>foo<!>()
