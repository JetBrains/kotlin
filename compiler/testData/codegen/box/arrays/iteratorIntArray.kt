// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val a = IntArray(5)
    val x = a.iterator()
    var i = 0
    while (x.hasNext()) {
        if (a[i] != x.next()) return "Fail $i"
        i++
    }
    return "OK"
}
