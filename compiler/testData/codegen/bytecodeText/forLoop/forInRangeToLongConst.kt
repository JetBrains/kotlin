const val N = 42L

fun test(): Long {
    var sum = 0L
    for (i in 1L .. N) {
        sum += i
    }
    return sum
}

// 1 LCMP
// 0 IFEQ
// 1 IFGT