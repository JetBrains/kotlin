// TARGET_BACKEND: JVM
// FILE: main.kt

@JvmOverloads
inline fun test1(i: Int = callCrossinlineInt() { 4 }): Int = i

inline fun callCrossinlineInt(crossinline lambda: () -> Int): Int = 3 + lambda()

@JvmOverloads
inline fun test2(i: Int = 5): Int {
    try {
        callInt() {
            return i + 6
        }
        return -1
    } finally {
        println("finally")
    }
}

inline fun callInt(lambda: () -> Int): Int = 3 + lambda()

@JvmOverloads
suspend inline fun test3(i: Int = 5): Int {
    try {
        callIntSuspend() {
            return i + 6
        }
        return -1
    } finally {
        callIntSuspend() { 7 }
    }
}

suspend inline fun callIntSuspend(lambda: suspend () -> Int): Int = 3 + lambda()

fun box() = "OK"
