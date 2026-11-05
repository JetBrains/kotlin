// CHECK_BYTECODE_LISTING

@file:Suppress("INLINE_CLASS_DEPRECATED", "INLINE_CLASS_DEPRECATED_ERROR")

inline class ICInt<T: Int>(val value: T)

inline class ICAny<T: Any>(val value: T?)

fun box(): String {
    var res = ICInt(1).value
    if (res != 1) return "FAIL 1: $res"
    return ICAny("OK").value ?: "FAIL 2: null"
}
