// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 forEach
// 0 forEachIndexed
fun box(): String {
    val sequence = generateSequence({ 1 }) { null }
    sequence.forEach {}
    sequence.forEachIndexed { i, v -> }
    return "OK"
}
