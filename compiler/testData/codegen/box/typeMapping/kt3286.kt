fun test(x: Int): String = when(x) {
        0 -> "zero"
        1 -> "one"
        2 -> "two"
        else -> blowUpHorribly()
    }

fun blowUpHorribly(): Nothing = throw RuntimeException("Blow up!")

fun box(): String {
    test(1)
    return "OK"
}