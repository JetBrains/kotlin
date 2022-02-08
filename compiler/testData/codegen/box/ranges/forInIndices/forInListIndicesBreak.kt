// WITH_STDLIB

fun test(xs: List<String>): String {
    var r = ""
    for (i in xs.indices) {
        if (i > 1) break
        r += xs[i]
    }
    return r
}

fun box(): String =
    test(listOf("O", "K", "2", "3", "4"))
