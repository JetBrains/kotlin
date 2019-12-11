// !CHECK_TYPE
// !DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -VARIABLE_EXPECTED

inline fun inlineFun(s: (p: Int) -> Unit) {
    inlineFun(s)
}

inline fun <T> inlineFun(s: T) {
    inlineFun<Int>(11)
}


inline fun <T> Function0<T>.inlineExt() {
    (checkSubtype<Function0<Int>>({11})).inlineExt();
    {11}.inlineExt()
}

inline operator fun <T, V> Function1<T, V>.not() : Boolean {
    return !this
}

inline operator fun <T, V> Function1<T, V>.inc() : Function1<T, V> {
    return this++
}