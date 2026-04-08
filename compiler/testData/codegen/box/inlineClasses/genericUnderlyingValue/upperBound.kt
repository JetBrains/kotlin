// CHECK_BYTECODE_LISTING
// LANGUAGE: +GenericInlineClassParameter
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

inline class ICString<T: String>(val value: T)

fun box(): String = ICString("OK").value