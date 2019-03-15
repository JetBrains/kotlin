// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    val x = +l[0]
    if (x != 1) return "Fail: $x}"
    return "OK"
}