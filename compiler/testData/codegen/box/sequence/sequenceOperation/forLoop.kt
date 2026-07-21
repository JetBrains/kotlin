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
    return result
}
