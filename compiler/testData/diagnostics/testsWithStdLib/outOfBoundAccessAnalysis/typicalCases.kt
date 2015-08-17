fun append(): IntArray {
    val a1 = IntArray(3)
    val a2 = IntArray(10)
    val res = IntArray(a1.size() + a2.size())
    var r = 0
    for (i in 0 .. a2.size() - 1) { // typo - a2 instead a1
        res[r] = <!OUT_OF_BOUND_ACCESS!>a1[i]<!>
        ++r
    }
    for (i in 0 .. a2.size()) { // user forgot "- 1"
        res[r] = <!OUT_OF_BOUND_ACCESS!>a2[i]<!>
        ++r
    }
    return res
}