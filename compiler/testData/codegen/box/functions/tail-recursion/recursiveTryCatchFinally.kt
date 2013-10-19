tailRecursive fun test(x : Int, b : Any, a : Any) : Int {
    if (x > 0) {
        try {
            dummy()
        } catch (e : Exception) {
            test(0, "catch", "no tail")
        } finally {
            return test(x - 1, "finally", "partial tail")
        }
    }
    return 1
}

fun dummy() = throw IllegalArgumentException()

fun box() : String = if (test(100000, "test", "test") == 1) "OK" else "FAIL"