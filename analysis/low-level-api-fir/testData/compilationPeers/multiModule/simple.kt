// MODULE: lib
// FILE: lib.kt
inline fun lib(): String = "OK"

// MODULE: main(lib)
// FILE: main.kt
fun main() {
    lib()
}