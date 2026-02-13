// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
<!POSSIBLE_DEADLOCK("object B : Any")!>object A<!> {
    val a = 1
    <!UNINITIALIZED_PROPERTY!>val b = <!UNINITIALIZED_ACCESS("val a: Int")!>B.a<!><!>
}

<!POSSIBLE_DEADLOCK("object A : Any")!>object B<!> {
    <!UNINITIALIZED_PROPERTY!>val a = A.a<!>
}
