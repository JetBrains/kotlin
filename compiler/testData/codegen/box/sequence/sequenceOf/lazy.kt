// WITH_STDLIB

// CHECK_BYTECODE_TEXT
fun box(): String {
    val seq = sequenceOf(0).map { it / 0 }
    return "OK"
}
