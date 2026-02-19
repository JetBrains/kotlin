// ISSUE: KT-68568

// MODULE: rootLib
// FILE: Some.kt
interface Some

// MODULE: lib(rootLib)
// FILE: Base.kt
interface Base {
    fun foo(s: Some)
}

// MODULE: app(lib)
// FILE: main.kt
class Derived(delegate: Base) : Base by delegate

fun box() = "OK"
