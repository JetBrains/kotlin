// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt
// the arguments of `take` and `drop` are evaluated and checked even if the sequence is empty
var calls = 0
fun count(value: Int): Int {
    calls++
    return value
}

fun box(): String {
    emptySequence<Int>().take(count(2)).forEach {}
    for (x in emptySequence<Int>().drop(count(1))) {}
    try {
        emptySequence<Int>().take(count(-1)).forEach {}
        return "Fail: no exception"
    } catch (e: IllegalArgumentException) {
        if (e.message != "Requested element count -1 is less than zero.") return "Fail: ${e.message}"
    }
    return if (calls == 3) "OK" else "Fail: calls=$calls"
}
