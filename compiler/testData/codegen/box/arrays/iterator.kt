// IGNORE_BACKEND: JS_IR
fun box(): String {
    val x = Array<Int>(5, { it } ).iterator()
    var i = 0
    while (x.hasNext()) {
        if (x.next() != i) return "Fail $i"
        i++
    }
    return "OK"
}
