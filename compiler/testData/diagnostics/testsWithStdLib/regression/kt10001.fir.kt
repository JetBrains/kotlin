fun foo1(p: Pair<Int?, Int>): Int {
    if (p.first != null) return p.first<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    return p.second
}

fun foo2(p: Pair<Int?, Int>): Int {
    if (p.first != null) return p.first
    return p.second
}
