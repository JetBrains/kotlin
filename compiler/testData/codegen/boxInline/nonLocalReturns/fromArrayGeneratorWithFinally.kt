// FILE: 1.kt
inline fun runReturning(f: () -> Nothing): Nothing = f()

// FILE: 2.kt
fun box(): String {
    var r = ""
    val x = try {
        Array<String>(1) {
            try {
                runReturning { return@Array "OK" }
            } finally {
                r += "O"
            }
        }[0]
    } finally {
        r += "K"
    }
    return r
}
