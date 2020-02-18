// FILE: 1.kt
inline fun runReturning(f: () -> Nothing): Nothing = f()

// FILE: 2.kt
fun box(): String {
    var r = ""
    val x = try {
        Array<String>(1) ext@{
            try {
                Array<String>(1) {
                    try {
                        runReturning { return@ext "OK" }
                    } finally {
                        r += "1"
                    }
                }[0]
            } finally {
                r += "2"
            }
        }[0]
    } finally {
        r += "3"
    }
    return if (r == "123") x else r
}
