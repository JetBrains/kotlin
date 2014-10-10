fun box(): String {
    val l = java.util.ArrayList<Int>()
    l.add(1)
    val x = l[0] < 2
    if (x != true) return "Fail: $x}"
    return "OK"
}