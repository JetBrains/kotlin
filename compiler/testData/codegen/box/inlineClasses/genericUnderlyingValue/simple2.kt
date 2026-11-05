// CHECK_BYTECODE_LISTING

inline value class ICAny<T: Any>(val value: T?)

fun box(): String = ICAny("OK").value.toString()
