fun test(a: Int, b: Int): Int {
    var sum = 0
    for (i in a until b) {
        sum = sum * 10 + i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 IF_ICMPGE
// 1 IF