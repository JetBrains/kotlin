// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    var x = l[0]
    x = 2
    if (x != 2) return "Fail: $x}"
    return "OK"
}