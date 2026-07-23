// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 LOOKUPSWITCH
// 0 TABLESWITCH
fun box(): String {
    val seq = sequenceOf(0).map { it / 0 }
    return "OK"
}
