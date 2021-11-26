// WITH_STDLIB

fun test(xs: List<String>): String {
    var r = ""
    for (i in xs.indices) {
        if (i % 2 == 0) continue
        r += xs[i]
    }
    return r
}

fun box(): String =
    test(listOf("0", "O", "2", "K", "4"))
