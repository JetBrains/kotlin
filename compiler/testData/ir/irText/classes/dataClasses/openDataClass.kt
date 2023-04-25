// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// This test emulates 'allopen' compiler plugin.

@Suppress("INCOMPATIBLE_MODIFIERS")
open data class ValidatedProperties(
    open val test1: String,
    open val test2: String
)
