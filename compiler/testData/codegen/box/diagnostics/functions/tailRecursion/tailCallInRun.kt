tailrec fun countDown(n: Int): Int {
    if (n <= 0) return 0
    run {
        return countDown(n - 1)
    }
}

fun box(): String {
    // Deep enough to blow the stack if tail recursion were NOT optimized.
    val result = countDown(1_000_000)
    if (result != 0) return result.toString()
    return "OK"
}
