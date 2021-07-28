fun <T : Collection<*>> test(c: T) {
    var sum = 0
    for (i in c.indices) {
        sum += i
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 1 INVOKEINTERFACE java/util/Collection\.size \(\)I

// 1 IF_ICMPGE
// 0 IF_ICMPGT
// 0 IF_ICMPLE
// 1 IF
