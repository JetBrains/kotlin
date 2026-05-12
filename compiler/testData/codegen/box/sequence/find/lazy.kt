// WITH_STDLIB

fun box(): String {
    val seq = generateSequence(3) { it - 1 }
    val x = seq.find { 1 / it == 1 }
    if (x != 1) return "failed: result of find was $x"
    return "OK"
}
