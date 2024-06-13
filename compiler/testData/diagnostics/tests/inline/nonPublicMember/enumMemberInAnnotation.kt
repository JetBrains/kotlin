// FIR_IDENTICAL
// ISSUE: KT-67810
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB

@Retention(AnnotationRetention.SOURCE)
internal annotation class A(val value: Value) {
    enum class Value {
        X, Y
    }
}

@A(A.Value.X)
inline fun foo() {
}
