// CHECK_BYTECODE_LISTING

inline value class ICString<T: String>(val value: T)

fun box(): String = ICString("OK").value
