// CHECK_BYTECODE_LISTING
// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter

inline class ICAny<T>(val value: T)

fun box(): String = ICAny("OK").value