// CHECK_BYTECODE_LISTING
// LANGUAGE: +GenericInlineClassParameter
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// IGNORE_BACKED: JVM

inline class ICAny<T: Any>(val value: T?)

fun box(): String = ICAny("OK").value.toString()