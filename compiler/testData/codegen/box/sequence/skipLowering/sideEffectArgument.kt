// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun box(): String {
    var counter = 0
    val seq = generateSequence(counter++) { it + 1 }.take(10)
    for (i in seq) {}
    return if (counter == 1) "OK" else "Fail: counter = $counter"
}
