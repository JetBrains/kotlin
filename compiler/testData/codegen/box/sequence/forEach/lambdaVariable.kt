// WITH_STDLIB

fun box(): String {
    var result = ""
    val predicate: (Any) -> Unit = { x ->
        if (x is String) result += x
    }
    sequenceOf("O", 2, "K").forEach(predicate)
    var result2 = ""
    val seq2 = sequenceOf(3, 2, 1).forEachIndexed { i, v -> result2 += i.toString() + v.toString() }
    if (result2 != "031221") return "fail: $result2"
    return result
}
