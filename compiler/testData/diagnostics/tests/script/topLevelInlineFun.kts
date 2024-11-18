// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

inline fun foo(f: () -> Unit) {
    f()
}
