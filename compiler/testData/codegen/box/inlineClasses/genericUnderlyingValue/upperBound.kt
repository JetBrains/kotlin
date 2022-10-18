// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter

inline class ICString<T: String>(val value: T)

fun box(): String = ICString("OK").value