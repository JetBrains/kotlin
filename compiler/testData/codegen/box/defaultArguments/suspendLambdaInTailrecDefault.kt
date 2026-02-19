// ISSUE: KT-82788
// WITH_STDLIB

tailrec fun test(n: Int, block: suspend () -> Unit = {}) : String {
    if (n == 0) return "OK"
    return test(n - 1)
}

fun box(): String {
    return test(2)
}