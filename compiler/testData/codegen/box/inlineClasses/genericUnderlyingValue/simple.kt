// CHECK_BYTECODE_LISTING
// LANGUAGE: +GenericInlineClassParameter
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// IGNORE_BACKED: JVM

inline class ICAny<T>(val value: T)

fun box(): String = ICAny("OK").value