// FIR_IDENTICAL

fun testScalar(a: Any): IntArray {
    if (a !is Int) return intArrayOf()
    return intArrayOf(a)
}

fun testSpread(a: Any): IntArray {
    if (a !is IntArray) return intArrayOf()
    return intArrayOf(*a)
}
