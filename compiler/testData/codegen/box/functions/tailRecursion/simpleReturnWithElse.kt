tailRecursive fun test(x : Int, a : Any) : Int {
    if (x == 0) {
        return 0
    } else if (x == 10) {
        test(0, "no tail")
        return 1 + test(x - 1, "no tail")
    } else {
        return test(x - 1, "tail")
    }
}

fun box() : String = if (test(1000000, "test") == 1) "OK" else "FAIL"