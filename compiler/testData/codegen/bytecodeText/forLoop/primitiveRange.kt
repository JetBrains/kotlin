// IGNORE_BACKEND: JVM_IR
fun f(r: IntRange) {
    for (i in r) {
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 1 getFirst
// 1 getLast