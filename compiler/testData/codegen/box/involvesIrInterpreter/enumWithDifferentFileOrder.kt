// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-73621: EVALUATED{IR} is missing
// FILE: main.kt
fun box() = Base1.OK.<!EVALUATED{IR}("OK")!>name<!>

// FILE: lib.kt
enum class Base1 { OK }
