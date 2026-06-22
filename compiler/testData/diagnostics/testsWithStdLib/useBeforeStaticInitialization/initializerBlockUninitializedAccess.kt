// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>object A<!> {
    val x = 1
    init {
        println(<!POTENTIALLY_UNINITIALIZED_ACCESS!>B.y<!>)
    }
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object B<!> {
    val x = A.x
    val y = "foo"
}
