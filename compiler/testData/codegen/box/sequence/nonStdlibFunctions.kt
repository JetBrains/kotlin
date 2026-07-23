// WITH_STDLIB

fun Sequence<Int>.first(): Int {
    return 999
}

fun box(): String {
    val seq = sequenceOf(1, 2, 3)

    val result = seq.first()

    return if (result != 999) "Fail: first() returned $result" else "OK"
}
