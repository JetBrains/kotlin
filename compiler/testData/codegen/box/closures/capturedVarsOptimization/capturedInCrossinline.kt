// FILE: lib.kt
inline fun runCrossInline(crossinline f: () -> Unit) {
    f()
}

// FILE: main.kt
fun box(): String {
    var x = ""
    runCrossInline { x = "OK" }
    return x
}