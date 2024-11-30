// IGNORE_GLOBAL_METADATA
// ^^^ absent EVALUATED{IR} diagnostics

// FILE: main.kt
fun box() = Base1.OK.<!EVALUATED{IR}("OK")!>name<!>

// FILE: lib.kt
enum class Base1 { OK }
