import test.*

fun sameName(s: Long): Long {
    return call {
        s
    }
}

fun sameName(s: Int): Int {
    return call {
        s
    }
}

fun box(): String {
    val result = sameName(1.toLong())
    if (result != 1.toLong()) return "fail1: ${result}"

    val result2 = sameName(2)
    if (result2 != 2) return "fail2: ${result2}"

    return "OK"
}