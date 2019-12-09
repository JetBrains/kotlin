fun test(b: Byte, s: Short, i: Int, l: Long) {
    val x1 = 1.rangeTo(b)
    val x2 = 1.rangeTo(s)
    val x3 = 1.rangeTo(i)
    val x4 = 1.rangeTo(l)

    val x5 = b.rangeTo(1)
    val x6 = s.rangeTo(1)
    val x7 = i.rangeTo(1)
    val x8 = l.rangeTo(1)
}