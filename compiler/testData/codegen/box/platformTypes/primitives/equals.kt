// IGNORE_BACKEND: JS_IR
fun box(): String {
    val l = ArrayList<Int>()
    l.add(1)
    val x = l[0] == 2
    if (x != false) return "Fail: $x}"
    return "OK"
}