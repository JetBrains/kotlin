fun mergeOk(): IntArray {
    val a1 = IntArray(3)
    val a2 = IntArray(10)
    val res = IntArray(a1.size() + a2.size())
    var i1 = 0
    var i2 = 0
    var r = 0
    while (i1 < a1.size() && i2 < a2.size()) {
        if (a1[i1] < a2[i2]) {
            res[r] = a1[i1]
            ++i1
        }
        else {
            res[r] = a2[i2]
            ++i2
        }
        ++r;
    }
    if (i1 < a1.size()) {
        for (i in i1 .. a1.size() - 1) {
            res[r] = a1[i]
            ++r
        }
    }
    else {
        for (i in i2 .. a2.size() - 1) {
            res[r] = a2[i]
            ++r
        }
    }
    return res
}