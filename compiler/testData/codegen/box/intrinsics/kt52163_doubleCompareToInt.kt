fun test1(): Int {
    val d: Double?
    d = 8.3
    return d.compareTo(8)
}

fun test2(): Int {
    val d: Double
    d = 8.3
    return d.compareTo(8)
}

fun box(): String {
    if (test1() != 1) return "Fail test1"
    if (test2() != 1) return "Fail test2"
    return "OK"
}
