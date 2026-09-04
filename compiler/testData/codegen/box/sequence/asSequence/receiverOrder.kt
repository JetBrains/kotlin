// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var log = ""
fun make(): List<Int> { log += "a"; return listOf(1) }
fun box(): String {
    val s = make().asSequence()
    log += "b"
    s.toList()
    return if (log == "ab") "OK" else "Fail: $log"
}
