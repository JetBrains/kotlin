// WITH_STDLIB

fun blockCall(block: () -> Int): Int = block()

tailrec fun test(n: Int, acc: Int): Int =
    if (n > 0) {
        val s = blockCall { acc + 1 }
        test(n - 1, s)
    } else {
        acc
    }


fun box(): String {
    val result = test(2, 0)
    if (result != 2) return "Fail: result = $result"
    return "OK"
}