// WITH_STDLIB

fun box(): String {
    val log = ArrayList<String>()

    fun source(): Sequence<Int> {
        log += "source"
        return sequenceOf(1)
    }

    val sequence = source().map {
        log += "map"
        it
    }

    check(log == listOf("source"))

    log += "terminal"
    check(sequence.toList() == listOf(1))
    check(log == listOf("source", "terminal", "map"))

    return "OK"
}
