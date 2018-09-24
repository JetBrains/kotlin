object Host {
    const val M = 1
    const val N = 4
}

fun test(): Int {
    var s = 0
    for (i in Host.M .. Host.N) {
        s += i
    }
    return s
}

// 0 IF_ICMPEQ
// 1 IF_ICMPGT