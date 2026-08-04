// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 forEach
fun box(): String {
    val sequence = generateSequence({ 1 }) { null }
    sequence.forEach {}
    return "OK"
}
