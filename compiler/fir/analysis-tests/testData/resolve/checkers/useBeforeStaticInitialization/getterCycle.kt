// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
object A {
    <!UNINITIALIZED_PROPERTY!>val x = <!UNINITIALIZED_ACCESS("val y: Any")!>y<!><!>
    <!UNINITIALIZED_PROPERTY!>val y: Any get() = <!UNINITIALIZED_ACCESS("val x: Any")!>x<!><!>
}