// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    var x = l[0]
    var y = l[0]
    l[0]--
    --l[0]
    x--
    --y
    if (l[0] != -1) return "Fail: ${l[0]}"
    if (x != 0) return "Fail x: $x"
    if (y != 0) return "Fail y: $y"
    return "OK"
}