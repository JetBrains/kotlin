// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 LOOKUPSWITCH
fun box(): String {
    val seq = sequenceOf(0).take(0).map { it / 0 }
    for (i in seq){}
    return "OK"
}
