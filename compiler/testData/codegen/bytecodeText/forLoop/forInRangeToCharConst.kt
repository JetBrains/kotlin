const val N = 'Z'

fun test(): Int {
    var sum = 0
    for (i in 'A' .. N) {
        sum += i.toInt()
    }
    return sum
}

// 0 IF_ICMPEQ
// 1 IF_ICMPGT