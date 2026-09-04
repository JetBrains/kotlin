// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.take
var log = ""
fun seed(): Int { log += "s"; return 1 }
fun n(): Int { log += "n"; return 2 }
fun box(): String {
    val r = generateSequence(seed()) { it + 1 }.take(n()).toList()
    if (r != listOf(1, 2)) return "Fail: $r"
    return if (log == "sn") "OK" else "Fail order: $log"
}
