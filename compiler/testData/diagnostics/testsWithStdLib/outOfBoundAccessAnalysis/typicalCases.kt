fun append(): IntArray {
    val a1 = IntArray(3)
    val a2 = IntArray(10)
    val res = IntArray(a1.size() + a2.size())
    var r = 0
    for (i in 0 .. a2.size() - 1) { // typo - a2 instead a1
        res[r] = <!OUT_OF_BOUND_ACCESS!>a1[i]<!>
        ++r
    }
    for (i in 0 .. a2.size()) { // user forgot "- 1"
        res[r] = <!OUT_OF_BOUND_ACCESS!>a2[i]<!>
        ++r
    }
    return res
}

fun setLast() {
    val arr = Array(3, { 1 })
    arr[arr.size() - 1] = 0                           // correct
    <!OUT_OF_BOUND_ACCESS!>arr[arr.size()] = 0<!>     // `-1` is omitted
}

fun setLast2(cond: Boolean) {
    var size = 3
    if (cond) { size = 0 }
    val arr = Array(size, { it })
    <!OUT_OF_BOUND_ACCESS!>arr[arr.size() - 1] = 0<!>         // arr.size() can return 0
}

fun listAppend() {
    val arr1 = arrayListOf(1, 2, 3, 4)
    val arr2 = Array(3, { it })
    for (i in 0.. arr2.size() - 1) {
        arr1.add(arr2[i])
    }
    arr1.get(500)                       // no info about `arr1` size, so no warning here
}

fun listAppend2(resetNeeded: Boolean, notTwo: Boolean, appendLineEnd: Boolean) {
    val arr = arrayListOf('!', '!')
    if (resetNeeded) {
        arr.clear()
    }
    if (notTwo) {
        arr.add('!')
    }
    if (appendLineEnd) {
        arr.add('\n')
    }
    println(arr)
    <!OUT_OF_BOUND_ACCESS!>arr[0] = '?'<!>
    <!OUT_OF_BOUND_ACCESS!>arr[1] = '?'<!>            // `arr` size can be 1
    println(arr)
}