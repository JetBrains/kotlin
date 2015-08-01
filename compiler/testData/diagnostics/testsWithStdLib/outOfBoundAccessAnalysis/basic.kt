fun accessWithLiteral(): Int {
    val arr = arrayOf(1)
    val a = arr[0]
    val b = <!OUT_OF_BOUND_ACCESS!>arr[1]<!>
    return a + b
}