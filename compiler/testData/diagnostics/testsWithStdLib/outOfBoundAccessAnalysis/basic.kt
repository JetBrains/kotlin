fun accessWithLiteral(): Int {
    val arr = arrayOf(1)
    val a = arr[0]
    val b = <!OUT_OF_BOUND_ACCESS!>arr[1]<!>
    return a + b
}

fun simpleFor() {
    val arr = IntArray(5)
    for (i in 0..4) {
        println(arr[i])
    }
    for (i in 0..5) {
        println(<!OUT_OF_BOUND_ACCESS!>arr[i]<!>)
    }
}