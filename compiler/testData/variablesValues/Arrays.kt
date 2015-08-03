fun simpleArray() {
    val arr1 = Array(3, { x -> x })
    val arr2: Array<Int>
    arr2 = arrayOf(1, 2)
    42
}


fun arrayWithTwoPossibleSizes(cond: Boolean) {
    val arr: Array<Boolean>
    if (cond) {
        arr = Array(11, { x -> true })
    }
    else {
        arr = arrayOf(false, false)
    }
    42
}

fun sizeMethodCall() {
    val arr = arrayOf(1, 2, 3)
    val sz = arr.size()
}