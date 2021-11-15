// WITH_STDLIB

fun test(xs: List<String>): String {
    var r = ""
    for (i in xs.indices) {
        r += xs[i]
    }
    return r
}

fun box(): String =
    test(listOf("OK"))
