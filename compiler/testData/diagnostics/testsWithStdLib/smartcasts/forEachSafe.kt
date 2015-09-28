// KT-7186: False "Type mismatch" error

fun indexOfMax(a: IntArray): Int? {
    var maxI: Int? = null
    a.forEachIndexed { i, value ->
        if (maxI == null || value >= a[<!TYPE_MISMATCH!>maxI<!>]) {
            maxI = i
        }
    }
    return maxI
}