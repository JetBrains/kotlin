fun Int.box(other: Int): String {
    if (this > other) {
        return "OK"
    } else {
        return "FAIL"
    }
}

fun box(): String = 99.box(9)