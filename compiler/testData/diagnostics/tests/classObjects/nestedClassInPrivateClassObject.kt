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

fun f1() = <!INVISIBLE_MEMBER!>A<!>.B.<!INVISIBLE_MEMBER!>C<!>

fun f2() = <!INVISIBLE_MEMBER!>A<!>.B.<!INVISIBLE_MEMBER!>C<!>.<!INVISIBLE_MEMBER!>foo<!>()
