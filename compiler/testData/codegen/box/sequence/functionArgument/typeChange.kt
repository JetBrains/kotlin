// WITH_STDLIB

fun test(seq: Sequence<Int>): String {
    val seq2 = seq.map { if (it == 1) "OK" else "FAIL" }
    for (item in seq2) {
        return item
    }
    return "SKIPPED BODY"
}

fun box(): String = test(sequenceOf(1))