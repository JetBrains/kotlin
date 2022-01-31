// CHECK_BYTECODE_LISTING
// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter
// IGNORE_BACKEND: JVM

inline class ICInt<T: Int>(val value: T)

inline class ICIcInt<T: ICInt<Int>>(val value: T)

fun box(): String {
    var res = ICInt(1).value
    if (res != 1) return "FAIL 1: $res"
    res = ICIcInt(ICInt(1)).value.value
    if (res != 1) return "FAIL 2: $res"
    return "OK"
}