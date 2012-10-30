fun box(): String {
    val x: Int? = 0
    if (x != 0) return "Fail $x"
    if (0 != x) return "Fail $x"
    if (!(x == 0)) return "Fail $x"
    if (!(0 == x)) return "Fail $x"
    return "OK"
}
