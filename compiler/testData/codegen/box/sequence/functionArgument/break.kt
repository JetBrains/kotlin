// WITH_STDLIB

fun test(seq: Sequence<Int>): String {
    val seq2 = seq.map { it * 2 }
    label@ for (j in 1..2) {
        for (i in seq) {
            break@label
        }
    }
    return "OK"
}

fun box(): String {
    val seq = sequenceOf(1, 2, 3)
    return test(seq)
}