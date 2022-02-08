// FIR_IDENTICAL
// !CHECK_TYPE
// !DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -VARIABLE_EXPECTED

inline fun inlineFun(s: (p: Int) -> Unit) {
    <!RECURSION_IN_INLINE!>inlineFun<!>(s)
}

inline fun <T> inlineFun(s: T) {
    <!RECURSION_IN_INLINE!>inlineFun<!><Int>(11)
}


inline fun <T> Function0<T>.inlineExt() {
    (checkSubtype<Function0<Int>>({11})).<!RECURSION_IN_INLINE!>inlineExt<!>();
    {11}.<!RECURSION_IN_INLINE!>inlineExt<!>()
}

inline operator fun <T, V> Function1<T, V>.not() : Boolean {
    return <!RECURSION_IN_INLINE!>!<!>this
}

inline operator fun <T, V> Function1<T, V>.inc() : Function1<T, V> {
    return this<!RECURSION_IN_INLINE!>++<!>
}