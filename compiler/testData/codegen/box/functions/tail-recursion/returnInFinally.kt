tailRecursive fun test(counter : Int, a : Any) : Int {
    if (counter == 0) return 0

    try {
        // do nothing
    } finally {
        return test(counter - 1, "no tail")
    }
}

fun box() : String = if (test(3, "test") == 0) "OK" else "FAIL"