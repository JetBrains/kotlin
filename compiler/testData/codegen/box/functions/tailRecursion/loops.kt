tailRecursive fun test(x : Int, a : Any) : Int {
    var z = if (x > 3) 3 else x
    while (z > 0) {
        if (z > 10) {
            return test(x - 1, "tail")
        }
        test(0, "no tail")
        z = z - 1
    }

    return 1
}

fun box() : String = if (test(100000, "test") == 1) "OK" else "FAIL"