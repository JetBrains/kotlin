fun box(): String {
    val l = java.util.ArrayList<Int>()
    l.add(1)
    val x = l[0].hashCode()
    if (x != 1) return "Fail: $x}"
    return "OK"
}