fun box(): String {
    val a = ByteArray(5)
    val x = a.iterator()
    var i = 0
    while (x.hasNext()) {
        if (a[i] != x.nextByte()) return "Fail $i"
        i++
    }
    return "OK"
}
