// WITH_STDLIB
// `generateSequence` without a seed can be iterated only once, also when it is reused inside a `sequence` builder

fun twice(): String {
    var calls = 0
    val s = sequence {
        val g = generateSequence { if (calls++ < 1) 1 else null }
        yieldAll(g)
        yieldAll(g)
    }
    return try {
        s.toList()
        "Fail: no exception, calls=$calls"
    } catch (e: IllegalStateException) {
        "OK"
    }
}

fun inRepeat(): String {
    var calls = 0
    val s = sequence {
        val g = generateSequence { if (calls++ < 1) 1 else null }
        repeat(2) { yieldAll(g) }
    }
    return try {
        s.toList()
        "Fail: no exception in repeat, calls=$calls"
    } catch (e: IllegalStateException) {
        "OK"
    }
}

fun box(): String {
    twice().let { if (it != "OK") return it }
    return inRepeat()
}
