// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(0).map { it / 0 }
    return "OK"
}
