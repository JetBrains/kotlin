<!POSSIBLE_DEADLOCK!>
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
val a = 1
<!UNINITIALIZED_PROPERTY!>val b = <!UNINITIALIZED_ACCESS!>A.b<!><!>

object A {
    val b = a
    <!UNINITIALIZED_PROPERTY!>val x = <!UNINITIALIZED_ACCESS("val c: String")!>c<!><!>
}

val c = "foo"
