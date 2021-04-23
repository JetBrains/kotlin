// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNNECESSARY_SAFE_CALL -UNNECESSARY_NOT_NULL_ASSERTION -CONFLICTING_JVM_DECLARATIONS
inline fun String.submit(action: Function1<Int, Int>) {

}

inline fun Function1<Int, Int>.submit() {
    this?.invoke(11)
    this!!.invoke(11)

    submit(this!!)
}

inline fun submit(action: Function1<Int, Int>) {
    <!USAGE_IS_NOT_INLINABLE!>action<!>?.invoke(10)
    <!USAGE_IS_NOT_INLINABLE!>action<!>!!.invoke(10)
}
