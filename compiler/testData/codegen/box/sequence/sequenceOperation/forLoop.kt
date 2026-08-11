// WITH_STDLIB

fun box(): String {
    var result = ""
    var shouldCapitalise = true
    val seq = sequence {
        yield("o")
        shouldCapitalise = false
        yield("K")
    }
    for (el in seq) {
        if (shouldCapitalise) result += el.uppercase()
        else result += el
    }
    var result2 = ""
    for (el in seq) {
        result2 += el
    }
    if (result2 != "oK") return "Failed: expected oK, actual $result2"
    var value = 0
    fun foo(): Int {
        return value++
    }
    val list = sequence {
        yield(foo())
        foo()
        yield(foo())
    }.toList()
    if (list != listOf(0, 2)) return "Failed: expected [0, 2], actual $list"
    return result
}
