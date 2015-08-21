fun forBreak() {
    val arr = IntArray(3)
    for (i in 1..5) {
        <!OUT_OF_BOUND_ACCESS!>arr[i]<!>          // obvious out-of-bound access
    }
    for (i in 1..5) {
        if (i > 2) {
            break
            <!UNREACHABLE_CODE!>arr[400]<!>
        }
        arr[i]                                    // no alarm here
    }
    for (i in 3..5) {
        if (i > 4) {
            break
            <!UNREACHABLE_CODE!>arr[400]<!>
        }
        <!OUT_OF_BOUND_ACCESS!>arr[i]<!>          // `i` can still be 3
    }
    for (i in 1..5) {
        if (i == 2) {
            break
            <!UNREACHABLE_CODE!>arr[400]<!>
        }
        <!OUT_OF_BOUND_ACCESS!>arr[i]<!>          // false alarm, `i` is thought to be 5. In general it's not easy to handle this situation
    }
}

fun forContinue() {
    val arr = IntArray(3)
    for (i in 1..5) {
        if (i > 2) {
            continue
            <!UNREACHABLE_CODE!>arr[400]<!>
        }
        arr[i]                                    // no alarm here
    }
    for (i in 3..5) {
        if (i > 4) {
            continue
            <!UNREACHABLE_CODE!>arr[400]<!>
        }
        <!OUT_OF_BOUND_ACCESS!>arr[i]<!>          // `i` can still be 3
    }
    for (i in 1..5) {
        if (i == 2) {
            continue
            <!UNREACHABLE_CODE!>arr[400]<!>
        }
        <!OUT_OF_BOUND_ACCESS!>arr[i]<!>          // false alarm, `i` is thought to be 5. In general it's not easy to handle this situation
    }
}

fun whileBreak(cond: Boolean) {
    val arr = IntArray(3)
    while (cond) {
        var i = 100
        <!OUT_OF_BOUND_ACCESS!>arr[i]<!>
    }
    while (cond) {
        var i = 100
        if (i > 2) {
            break
        }
        <!UNREACHABLE_CODE!>arr[i]<!>
    }
}

fun whileContinue(cond: Boolean) {
    val arr = IntArray(3)
    while (cond) {
        var i = 100
        if (i > 2) {
            continue
        }
        <!UNREACHABLE_CODE!>arr[i]<!>
    }
}

fun nestedLoopsWithWhen() {
    val arr1 = Array(6, { it })
    val arr2 = Array(11, { it })
    val arr3 = Array(16, { it })
    for (i in 0 .. arr1.size() - 1)
        for (j in 0 .. arr2.size() - 1)
            for (k in 0 .. arr3.size() - 1)
                when {
                    k <= 5 && j <= 5 -> println("cube: ${arr1[i]} ${arr2[j]} ${arr3[k]}")
                    k <= 10 -> println("not cube: ${arr1[i]} ${arr2[j]} ${arr3[k]}")
                    else -> println("not cube at all: ${arr1[i]} ${arr2[j]} ${arr3[k]}")
                }

    for (i in 0 .. arr1.size() - 1)
        for (j in 0 .. arr2.size())             // `-1` is omitted
            for (k in 0 .. arr3.size())         // `-1` is omitted
                when {
                    k <= 5 && j <= 5 -> println("cube: ${arr1[i]} ${arr2[j]} ${arr3[k]}")
                    k <= 10 -> println("not cube: ${arr1[i]} ${<!OUT_OF_BOUND_ACCESS!>arr2[j]<!>} ${arr3[k]}")
                    else -> println("not cube at all: ${arr1[i]} ${<!OUT_OF_BOUND_ACCESS!>arr2[j]<!>} ${<!OUT_OF_BOUND_ACCESS!>arr3[k]<!>}")
                }
}