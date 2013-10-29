tailRecursive fun test(counter : Int, a : Any) : Int? {
    if (counter < 0) return null
    if (counter == 0) return 777

    return test(-1, "no tail") ?: test(-2, "no tail") ?: test(counter - 1, "tail")
}

fun box() : String =
    if (test(100000, "test") == 777) "OK"
    else "FAIL"