// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Inner(val result: String)

inline class A(val inner: Inner = Inner("OK"))

fun box(): String {
    return A().inner.result
}
