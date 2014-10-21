fun box(): String {
    val l = java.util.ArrayList<Int>()
    l.add(1)
    val x = l[0].toShort()
    if (x != 1.toShort()) return "Fail: $x}"
    return "OK"
}