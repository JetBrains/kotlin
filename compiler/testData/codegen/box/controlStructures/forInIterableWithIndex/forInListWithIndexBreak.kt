// WITH_STDLIB

fun test(xs: List<String>): String {
    var r = ""
    for ((i, x) in xs.withIndex()) {
        if (i > 1) break
        r += "$i:$x;"
    }
    return r
}

fun box(): String {
    val t = test(listOf("a", "b", "c", "d", "e"))
    if (t != "0:a;1:b;") return "Failed: $t"
    return "OK"
}