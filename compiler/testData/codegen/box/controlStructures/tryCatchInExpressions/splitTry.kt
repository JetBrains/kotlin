// IGNORE_BACKEND: JVM_IR
inline fun test(s: () -> Int): Int =
        try {
            val i = s()
            i + 10
        }
        finally {
            0
        }

fun box() : String {
    test {
        try {
            val p = 1
            return "OK"
        }
        catch(e: Exception) {
            -2
        }
        finally {
            -3
        }
    }

    return "Failed"
}
