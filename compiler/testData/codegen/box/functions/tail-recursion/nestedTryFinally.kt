tailRecursive fun test(x : Int, b : Any, a : Any) : Int {
    if (x > 0) {
        try {
            dummy()
        } finally {
            try {
                test(0, "try", "no tail")
            } finally {
                return test(x - 1, "finally", "partial tail")
            }
        }
    }
    else {
        return 1
    }
}

fun dummy() = 0

fun box() : String {
    if (test(1000000, "test", "test") == 1) {
        return "OK"
    }
    return "FAIL"
}
