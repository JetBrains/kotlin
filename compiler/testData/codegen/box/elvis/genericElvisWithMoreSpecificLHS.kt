// WITH_STDLIB

fun test(foo: MutableList<String>?): List<String> {
    val bar = foo ?: listOf()
    return bar
}

fun box(): String {
    val a = test(null)
    if (a.isNotEmpty()) return "Fail 1"

    val b = test(mutableListOf("a"))
    if (b.size != 1) return "Fail 2"

    return "OK"
}
