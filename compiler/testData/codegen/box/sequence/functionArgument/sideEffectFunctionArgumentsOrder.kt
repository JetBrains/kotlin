// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt
// the function arguments are evaluated in the order of the calls, like when the stdlib creates the sequence
var log = ""
fun <T> logged(name: String, value: T): T {
    log += name
    return value
}

fun box(): String {
    val result = sequenceOf(1, 2, 3).map(logged("a") { x: Int -> x + 1 }).take(logged("b", 2)).filter(logged("c") { x: Int -> x > 2 }).toList()
    if (log != "abc" || result != listOf(3)) return "Fail: log=$log result=$result"
    log = ""
    try {
        sequenceOf(1).take(logged("t", -1)).map(logged("m") { x: Int -> x }).toList()
        return "Fail: no exception"
    } catch (e: IllegalArgumentException) {
    }
    // the stdlib throws in `take` before `map` is called
    return if (log == "t") "OK" else "Fail: log=$log"
}
