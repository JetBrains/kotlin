// MODULE: lib
// FILE: lib.kt
inline fun lib(): String = "OK"

// MODULE: base(lib)
// FILE: base.kt
inline fun base(): String = lib()

// MODULE: main(base)
// FILE: main.kt
fun main() {
    base()
}