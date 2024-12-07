// FIR_IDENTICAL
// This test emulates 'allopen' compiler plugin.

@Suppress("INCOMPATIBLE_MODIFIERS")
open data class ValidatedProperties(
    open val test1: String,
    open val test2: String
)
