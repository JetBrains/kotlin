// WITH_RUNTIME

fun test(): Int {
    var sum = 0
    for (i in 4 downTo 1) {
        sum = sum * 10 + i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 IF_ICMPEQ
// 1 IF_ICMPLT