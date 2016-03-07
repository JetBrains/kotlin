// WITH_RUNTIME

fun exhaustive(x: Int): Int {
    var r: Int
    when (x) {
        1 -> r = 1
        2 -> r = 2
        3 -> r = 3
        else -> r = 4
    }

    return r
}

fun nonExhaustive(x: Int): Int {
    var r: Int = 4
    when (x) {
        1 -> r = 1
        2 -> r = 2
        3 -> r = 3
    }

    return r
}

fun box(): String {
    var result = (0..3).map(::exhaustive).joinToString()

    if (result != "4, 1, 2, 3") return "exhaustive:" + result

    result = (0..3).map(::nonExhaustive).joinToString()

    if (result != "4, 1, 2, 3") return "non-exhaustive:" + result
    return "OK"
}
