// WITH_STDLIB

fun box(): String {
    var result = ""
    val predicate: (Any) -> Unit = { x ->
        if (x is String) result += x
    }
    sequenceOf("O", 2, "K").forEach(predicate)
    return result
}
