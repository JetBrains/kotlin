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

fun f1() = A.<!INAPPLICABLE_CANDIDATE!>Companion<!>.<!UNRESOLVED_REFERENCE!>B<!>.<!UNRESOLVED_REFERENCE!>C<!>

fun f2() = A.<!INAPPLICABLE_CANDIDATE!>Companion<!>.<!UNRESOLVED_REFERENCE!>B<!>.<!UNRESOLVED_REFERENCE!>C<!>.<!UNRESOLVED_REFERENCE!>foo<!>()