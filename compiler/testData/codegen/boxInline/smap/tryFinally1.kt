// WITH_STDLIB

// FILE: 1.kt
inline fun g(block: () -> Unit) {
    block()
}

var x: String? = null

fun compute(): String {
    try {
        for (a in listOf("a")) {
            g {
                for (b in listOf("b")) {
                    return b
                }
            }
        }
    } finally {
        x = "OK"
    }
    return "FAIL"
}

// FILE: 2.kt

fun box(): String {
    val result = compute()
    if (result == "FAIL") return result
    return x ?: "FAIL"
}
