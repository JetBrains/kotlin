object A {
    object B {
        object A
    }
}

object B

val err = B.<!UNRESOLVED_REFERENCE!>A<!>.<!UNRESOLVED_REFERENCE!>B<!>
val correct = A.B.A