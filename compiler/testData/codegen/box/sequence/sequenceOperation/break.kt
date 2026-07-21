// WITH_STDLIB

fun box(): String {
    var result = ""
    val seq = sequence {
        yield(1)
        result = "failed: code shouldn't reach here"
        yield(2)
    }
    for (el in seq) {
        if (el == 1) break
        if (el == 2) result = "failed: didn't break"
    }
    result = "OK"
    return result
}
