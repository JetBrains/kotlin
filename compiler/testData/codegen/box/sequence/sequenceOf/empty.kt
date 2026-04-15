// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 LOOKUPSWITCH
// 1 TABLESWITCH
fun box(): String {
    val empty = sequenceOf<Int>()
    for (item in empty) {
        return "failed: empty sequence should not have item: $item"
    }
    val notEmpty = sequenceOf(1, 2, 3)
    for (item in notEmpty.filter { false }) {
        return "failed: filter { false } didn't filter $item"
    }
    return "OK"
}
