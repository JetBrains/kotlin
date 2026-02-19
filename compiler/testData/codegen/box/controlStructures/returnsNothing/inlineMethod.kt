// FILE: lib.kt
inline fun exit(): Nothing = null!!

// FILE: main.kt
fun box(): String {
    val a: String
    try {
        a = "OK"
    }
    catch (e: Exception) {
        exit()
    }
    return a
}