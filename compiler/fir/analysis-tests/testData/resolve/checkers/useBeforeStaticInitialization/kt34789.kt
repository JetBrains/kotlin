// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

// FILE: test1.kt
object A {
    <!UNINITIALIZED_PROPERTY!>val a = <!UNINITIALIZED_ACCESS!>test<!><!>
    val b = "test"
}

// FILE: test2.kt
<!UNINITIALIZED_PROPERTY!>val test = <!UNINITIALIZED_ACCESS!>A.b<!><!>
