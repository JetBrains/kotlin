// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
val a = 1
val b = A.b

object A {
    val b = a
    <!UNINITIALIZED_PROPERTY!>val x = <!UNINITIALIZED_ACCESS("val c: String")!>c<!><!>
}

<!UNINITIALIZED_PROPERTY!>val c = "foo"<!>
