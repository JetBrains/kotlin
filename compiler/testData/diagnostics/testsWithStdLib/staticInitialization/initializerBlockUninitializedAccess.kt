// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
<!POSSIBLE_DEADLOCK("object B : Any")!>object A<!> {
    val x = 1
    init {
        println(<!UNINITIALIZED_ACCESS("val y: String")!>B.y<!>)
    }
}

<!POSSIBLE_DEADLOCK("object A : Any")!>object B<!> {
    val x = A.x
    <!UNINITIALIZED_PROPERTY!>val y = "foo"<!>
}
