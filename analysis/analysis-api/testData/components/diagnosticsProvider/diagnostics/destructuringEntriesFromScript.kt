// KT-90376
// MODULE: lib
// MODULE_KIND: ScriptSource
// FILE: declarations.kts
data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

val c = 3

// MODULE: main(lib)
// FILE: main.kt
fun use() {
    a
    b
    c
}
