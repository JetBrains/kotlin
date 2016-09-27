fun foo1(p: Pair<Int?, Int>): Int {
    if (p.first != null) return p.first!!
    return p.second
}

fun foo2(p: Pair<Int?, Int>): Int {
    if (p.first != null) return <!SMARTCAST_IMPOSSIBLE!>p.first<!>
    return p.second
}
