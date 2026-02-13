// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
<!POSSIBLE_DEADLOCK!>object B<!> {
    val y = 5
    init {
        println("A = " + A)
        println(<!UNINITIALIZED_ACCESS("val y: String")!>A.y<!>)
    }
}

<!POSSIBLE_DEADLOCK!>object A<!> {
    val x = B.y
    <!UNINITIALIZED_PROPERTY!>val y = "test"<!>
    init {
        println("B = " + B)
    }
}
