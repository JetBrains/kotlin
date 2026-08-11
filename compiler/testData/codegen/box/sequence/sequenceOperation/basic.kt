// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 SequenceScope
// 0 kotlin/sequences/SequencesKt.first
// 0 kotlin/sequences/SequencesKt.last
fun box(): String {
    var result = ""
    val x = sequence {
        result += "O"
        yield(1)
        result += "NOT OK"
        yield(2)
    }.first()

    val y = sequence {
        yield(3)
        yield(4)
        result += "K"
    }.last()

    return result
}
