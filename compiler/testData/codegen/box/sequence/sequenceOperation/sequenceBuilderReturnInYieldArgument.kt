// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun f(flag: Boolean) = sequence { yield(1); yield(if (flag) 2 else return@sequence); yield(3) }.toList()

fun g(flag: Boolean): List<Int> {
    val l = listOf(2)
    return sequence { yield(1); yieldAll(if (flag) l else return@sequence); yield(3) }.toList()
}

fun box(): String {
    val r = "${f(true)} ${f(false)} ${g(true)} ${g(false)}"
    return if (r == "[1, 2, 3] [1] [1, 2, 3] [1]") "OK" else "Fail: $r"
}
