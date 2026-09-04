// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.take
var log = ""
var count = 1

fun receiver(): List<Int> {
    log += "r"
    count = 2
    return listOf(1, 2, 3)
}

fun count(): Int {
    log += "c"
    return count
}

fun box(): String {
    val calls = receiver().asSequence().take(count()).toList()
    if (calls != listOf(1, 2)) return "Fail call argument: $calls"
    if (log != "rc") return "Fail call order: $log"

    var n = 1
    val variable = run { n = 2; listOf(1, 2, 3) }.asSequence().take(n).toList()
    if (variable != listOf(1, 2)) return "Fail variable argument: $variable"
    return "OK"
}
