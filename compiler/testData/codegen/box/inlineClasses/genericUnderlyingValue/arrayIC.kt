// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// LANGUAGE: +GenericInlineClassParameter
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

inline class ICStr(val value: String)
inline class ICIStr<T : ICStr>(val value: T)
inline class ICIStrArray<T : ICStr>(val value: Array<T>)

fun box(): String {
    val res = ICIStrArray(arrayOf(ICStr("OK"))).value[0].value
    if (res != "OK") return res
    return ICIStr(ICStr("OK")).value.value
}