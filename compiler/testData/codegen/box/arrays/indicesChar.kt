// WITH_STDLIB

fun box(): String {
    val a = CharArray(5)
    val x = a.indices.iterator()
    while (x.hasNext()) {
        val i = x.next()
        if (a[i] != 0.toChar()) return "Fail $i ${a[i]}"
    }
    return "OK"
}
