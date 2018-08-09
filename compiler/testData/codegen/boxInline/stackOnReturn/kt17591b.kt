// FILE: 1.kt
inline fun alwaysOk(s: String, fn: (String) -> String): String {
    try {
        throw Exception()
    }
    catch(e: Exception) {
        fn(return "fail")
    }
    finally {
        fn(return "OK")
    }
}

// FILE: 2.kt
fun box() = alwaysOk("what?") { it }