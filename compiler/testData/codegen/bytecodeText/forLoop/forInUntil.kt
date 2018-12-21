// IGNORE_BACKEND: JVM_IR
fun test(): Int {
    var sum = 0
    for (i in 1 until 6) {
        sum = sum * 10 + i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 1 IF
