// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

inline fun <R> test(s: () -> R): R {
    var b = false
    try {
        return s()
    } finally {
        !b
    }
}

// FILE: 2.kt

fun box(): String {
    try {
        test {
            return@box "OK"
        }
    } finally {
    }

    return "fail"
}
