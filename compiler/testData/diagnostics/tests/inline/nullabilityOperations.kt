// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNNECESSARY_SAFE_CALL -UNNECESSARY_NOT_NULL_ASSERTION
inline fun String.submit(action: Function1<Int, Int>) {

}

inline fun Function1<Int, Int>.submit() {
    <!USAGE_IS_NOT_INLINABLE!>this<!>?.invoke(11)
    <!USAGE_IS_NOT_INLINABLE!>this<!>!!.invoke(11)

    submit(<!USAGE_IS_NOT_INLINABLE!>this<!>!!)
}

inline fun submit(action: Function1<Int, Int>) {
    <!USAGE_IS_NOT_INLINABLE!>action<!>?.invoke(10)
    <!USAGE_IS_NOT_INLINABLE!>action<!>!!.invoke(10)
}