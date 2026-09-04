// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var log = ""
fun n(): Int { log += "n"; return 1 }
fun dest(): MutableList<Int> { log += "d"; return mutableListOf() }
fun box(): String {
    sequenceOf(1, 2).take(n()).toCollection(dest())
    return if (log == "nd") "OK" else "Fail: $log"
}
