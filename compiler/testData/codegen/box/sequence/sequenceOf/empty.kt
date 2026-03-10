// WITH_STDLIB

fun box(): String {
    val empty = sequenceOf<Int>()
    for (item in empty) {
        return "fail"
    }
    return "OK"
}