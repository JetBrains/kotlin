// FIR_IDENTICAL
// IGNORE_BACKEND: JKLIB
fun box(): String {
    val a = DoubleArray(5)
    val x = a.iterator()
    var i = 0
    while (x.hasNext()) {
        if (a[i] != x.next()) return "Fail $i"
        i++
    }
    return "OK"
}
