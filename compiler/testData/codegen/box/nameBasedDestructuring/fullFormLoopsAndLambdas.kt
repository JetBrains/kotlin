// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

data class PairProps(val key: String, val value: Int)

fun box(): String {
    val list = listOf(PairProps("a", 1), PairProps("b", 2))

    var sum = 0
    for ((val key, val value) in list) {
        sum += value
    }
    if (sum != 3) return "FAIL"

    val lambdaResult = list.map { (val key, val value) -> "$key:$value" }
    if (lambdaResult.joinToString() != "a:1, b:2") return "FAIL"

    for ((val index, val element = value) in list.withIndex()) {
        if (element.value != index + 1) return "FAIL"
    }

    return "OK"
}
