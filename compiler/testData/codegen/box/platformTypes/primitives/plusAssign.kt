// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    var x = l[0]
    x += 1
    l[0] += 1
    if (l[0] != 2) return "Fail: ${l[0]}"
    if (x != 2) return "Fail: $x}"
    return "OK"
}