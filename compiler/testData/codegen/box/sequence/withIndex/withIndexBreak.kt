// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    var r = ""
    for ((i, v) in sequenceOf("a", "b", "c").withIndex()) {
        r += "$i$v"
        if (i == 0) break
    }
    return if (r == "0a") "OK" else "Fail: $r"
}
