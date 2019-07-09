// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val a = Array<Int>(5, {it})
    val x = a.indices.iterator()
    while (x.hasNext()) {
        val i = x.next()
        if (a[i] != i) return "Fail $i ${a[i]}"
    }
    return "OK"
}
