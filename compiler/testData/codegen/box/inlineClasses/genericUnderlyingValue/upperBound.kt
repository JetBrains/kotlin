// CHECK_BYTECODE_LISTING
// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter

inline class ICString<T: String>(val value: T)

fun box(): String = ICString("OK").value