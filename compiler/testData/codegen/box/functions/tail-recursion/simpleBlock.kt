tailRecursive fun test(x : Int, a : Any) : Int =
    if (x == 1) {
        test(x - 1, "no tail")
        1 + test(x - 1, "no tail")
    } else if (x > 0) {
        test(x - 1, "tail")
    } else {
        0
    }

fun box() : String = if (test(1000000, "test") == 1) "OK" else "FAIL"