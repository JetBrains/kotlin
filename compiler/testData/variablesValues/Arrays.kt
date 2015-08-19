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

fun multiSizeArray(cond: Boolean) {
    val arr: Array<Int>
    if (cond) {
        arr = Array(3, { it })
    }
    else {
        arr = Array(5, { it })
    }

    if (arr.size() == 5) {
        for (i in 3 .. 4) {
            arr[i] = 0
        }
    }
}