// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// LANGUAGE: +GenericInlineClassParameter
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

inline class ICIntArray<T: Int>(val value: Array<T>)

fun box(): String = if (ICIntArray(arrayOf(1)).value[0] == 1) "OK" else "FAIL"