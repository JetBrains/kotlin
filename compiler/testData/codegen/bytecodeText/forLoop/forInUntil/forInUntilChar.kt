fun test(a: Char, b: Char): String {
    var s = ""
    for (i in a until b) {
        s += i
    }
    return s
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// 1 IF_ICMPGE
// 0 IF_ICMPLT
// 1 IF
