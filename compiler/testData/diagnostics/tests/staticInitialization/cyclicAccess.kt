// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
<!POSSIBLE_DEADLOCK("object B : Any")!>object A<!> {
    <!UNINITIALIZED_PROPERTY!>val a: Any = <!UNINITIALIZED_ACCESS("val a: Any")!>B.a<!><!>
}

<!POSSIBLE_DEADLOCK("object A : Any")!>object B<!> {
    <!UNINITIALIZED_PROPERTY!>val a: Any = <!UNINITIALIZED_ACCESS("val a: Any")!>A.a<!><!>
}