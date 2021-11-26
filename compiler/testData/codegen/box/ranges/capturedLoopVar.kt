// WITH_STDLIB

fun build(): List<() -> Int> {
    val r = ArrayList<() -> Int>()
    for (i in 0 until 3) {
        r.add({ i })
    }
    return r
}

fun box(): String {
    val t = build().map { it() }
    if (t != listOf(0, 1, 2)) return "Failed: $t"
    return "OK"
}