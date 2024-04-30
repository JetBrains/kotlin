// ISSUE: KT-67810
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB

@Retention(AnnotationRetention.SOURCE)
internal annotation class A(val value: Value) {
    enum class Value {
        X, Y
    }
}

@A(A.Value.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>X<!>)
inline fun foo() {
}
