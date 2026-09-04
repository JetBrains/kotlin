// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var log = ""
fun n(): Int { log += "n"; return 1 }
fun box(): String {
    val r = generateSequence({ log += "s"; 1 }) { it + 1 }.take(n()).toList()
    return if (r == listOf(1) && log == "ns") "OK" else "Fail: $r, $log"
}
