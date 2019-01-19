// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    val x = l[0] * 2
    if (x != 2) return "Fail: $x}"
    return "OK"
}