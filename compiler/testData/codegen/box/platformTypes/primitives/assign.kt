// IGNORE_BACKEND: JS_IR
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    var x = l[0]
    x = 2
    if (x != 2) return "Fail: $x}"
    return "OK"
}