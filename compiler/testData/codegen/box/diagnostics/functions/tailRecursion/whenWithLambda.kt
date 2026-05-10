// ISSUES: KT-82196

tailrec fun withWhen(counter: Int): Int {
    return when (counter) {
        0 -> withWhen(1)
        1 -> counter
        else -> run { null } ?: withWhen(counter - 1)
    }
}

fun box(): String = if (withWhen(100000) == 1) "OK" else "FAIL"
