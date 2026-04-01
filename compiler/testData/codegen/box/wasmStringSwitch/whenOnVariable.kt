// WITH_STDLIB

object D {
    var d = 0
}

fun foo(): String {
    D.d++
    return "ok"
}

fun box(): String {
    D.d = 0
    val b = foo()

    val res = when (b) {
        "" -> "FAIL1"
        "ok" -> "OK"
        "" -> "FAIL2"
        else -> "FAIL3"
    }

    if (D.d != 1) {
        return "FAIL4"
    }

    return res
}
