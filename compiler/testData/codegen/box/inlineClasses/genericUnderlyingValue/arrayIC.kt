// CHECK_BYTECODE_LISTING

inline value class ICStr(val value: String)
inline value class ICIStr<T : ICStr>(val value: T)
inline value class ICIStrArray<T : ICStr>(val value: Array<T>)

fun box(): String {
    val res = ICIStrArray(arrayOf(ICStr("OK"))).value[0].value
    if (res != "OK") return res
    return ICIStr(ICStr("OK")).value.value
}
