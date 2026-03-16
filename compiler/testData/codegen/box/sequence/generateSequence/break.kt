// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
fun box(): String {
    val seq = generateSequence(1) { if (it < 10) it + 1 else null }
    label@ for(j in 1..2) {
        for (i in seq.map { it * 2 }) {
            break@label
        }
    }
    return "OK"
}