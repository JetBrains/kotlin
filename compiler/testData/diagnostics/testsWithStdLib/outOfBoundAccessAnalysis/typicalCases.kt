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
    <!OUT_OF_BOUND_ACCESS!>arr[0] = '?'<!>
    <!OUT_OF_BOUND_ACCESS!>arr[1] = '?'<!>            // `arr` size can be 1
    println(arr)
}

fun informationLost(u: Int) {
    val arr = Array(10, { it })
    for (i in 0 .. arr.size()) {
        if (i < u) {
            println("fst: ${arr[i]}")
        }
        else {
            println("snd: ${arr[i]}")
        }
        println("after: ${arr[i + 500] + 500}")     // no warning, because we lose info in such cases, `i` us undefined after such if-else =(
    }
}

fun processingInLoop() {
    val arr = Array(10, { it })
    val bound = 5
    for (i in 0 .. arr.size()) {
        if (i > bound) {
            break
        }
        println(arr[i])             // no alarm due to `break` above
    }
}

fun closures() {
    val arr = arrayOf(1, 2)
    var a = 100
    <!OUT_OF_BOUND_ACCESS!>arr[a]<!>
    { a = 999 }()
    arr[a]              // no false alarm, `a` is undefined after lambda call

    a = 100
    fun updatesA1() { a = 999 }
    <!OUT_OF_BOUND_ACCESS!>arr[a]<!>
    updatesA1()
    arr[a]              // no false alarm, `a` is undefined after call

    a = 100
    val updatesA2: () -> Unit = { a = 999 }
    <!OUT_OF_BOUND_ACCESS!>arr[a]<!>
    updatesA2()
    arr[a]              // no false alarm

    a = 100
    val updatesA3 = {
        val arr2 = arrayOf(1)
        var c = 100
        fun updatesA4() {
            a = 999
            c = 999
        }
        <!OUT_OF_BOUND_ACCESS!>arr2[c]<!>
        updatesA4()
        arr2[c]         // no false alarm
    }
    <!OUT_OF_BOUND_ACCESS!>arr[a]<!>
    updatesA3()
    arr[a]              // no false alarm
}