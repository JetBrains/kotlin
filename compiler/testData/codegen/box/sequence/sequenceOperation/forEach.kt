// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 SequenceScope
// 0 kotlin/sequences/SequencesKt.forEach
fun box(): String {
    var result = ""
    sequence {
        yield("O")
        yield("K")
    }.forEach { result += it }
    return result
}
