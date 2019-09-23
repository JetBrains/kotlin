fun fib_rec(i: Int): Int {
    if (i == 0) return 0
    if (i == 1) return 1
    return fib_rec(i - 1) + fib_rec(i - 2)
}

fun fib_loop(i: Int): Int {
    var a = 0
    var b = 1
    if (i == 0) return a
    if (i == 1) return b
    for (j in 2..i) {
        val tmp = a
        a = b
        b = a + tmp
    }
    return b
}

fun box(): String {
    val fib_rec = fib_rec(20)
    val fib_loop = fib_loop(20)
    if (fib_loop == fib_rec) {
        return "OK"
    } else {
        return "FAIL"
    }
}