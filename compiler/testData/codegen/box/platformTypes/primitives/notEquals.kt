// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    val x = l[0] != 1
    if (x != false) return "Fail: $x}"
    return "OK"
}