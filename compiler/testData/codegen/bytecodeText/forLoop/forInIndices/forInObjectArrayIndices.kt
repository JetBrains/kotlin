fun test() {
    var sum = 0
    for (i in arrayOf("", "", "", "").indices) {
        sum += i
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 1 IF_ICMPGE
// 1 IF