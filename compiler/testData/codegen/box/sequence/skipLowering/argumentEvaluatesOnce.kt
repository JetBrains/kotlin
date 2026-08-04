// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun box(): String {
    var counter = 0
    val sideEffect = { counter++; 1 }
    val seq = sequenceOf(sideEffect(), 2)
    for (i in seq) {}
    if (counter != 1) return "failed: counter was supposed to be 1, was $counter"
    return "OK"
}
