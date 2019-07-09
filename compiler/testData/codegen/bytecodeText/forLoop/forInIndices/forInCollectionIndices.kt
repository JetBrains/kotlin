// IGNORE_BACKEND: JVM_IR
fun test() {
    var sum = 0
    for (i in listOf(0, 0, 0, 0).indices) {
        sum += i
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast

// 0 IF_ICMPGT
// 0 IF_ICMPEQ
// 1 IF_ICMPGE
