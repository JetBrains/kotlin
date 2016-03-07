// WITH_RUNTIME

fun foo(x: Int): Int {
    return when (x) {
        2 -> 6
        1 -> 5
        3 -> 7
        else -> 8
    }
}

fun box(): String {
    var result = (0..3).map(::foo).joinToString()

    if (result != "8, 5, 6, 7") return "unordered:" + result
    return "OK"
}
