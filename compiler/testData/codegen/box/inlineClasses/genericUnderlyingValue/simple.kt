// CHECK_BYTECODE_LISTING

inline value class ICAny<T>(val value: T)

fun box(): String = ICAny("OK").value
