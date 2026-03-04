fun test() {
    val a: Any? = null
    val b: Any = ""

    // Elvis
    a ?: b

    // Boolean short-circuit
    true && false
    true || false

    // Identity equality
    a === b
    a !== b

    // Plain assignment
    var x = 1
    x = 2
}
