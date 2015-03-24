// See also KT-7186

fun IntArray.forEachIndexed( op: (i: Int, value: Int) -> Unit) {
    for (i in 0..this.size()-1)
        op(i, this[i])
}

fun max(a: IntArray): Int? {
    var maxI: Int? = null
    a.forEachIndexed { i, value ->
        if (maxI == null || value >= a[<!TYPE_MISMATCH!>maxI<!>])
            maxI = i
    }
    return maxI
}