// FILE: lib.kt
typealias F<T, R> = T.() -> R

inline fun <T, R> T.myRun(f: F<T, R>) = f()

// FILE: main.kt
fun box(): String {
    val x = "K"
    return "O".myRun { this + x }
}
