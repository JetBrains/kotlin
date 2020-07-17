// !LANGUAGE: +InlineClasses

inline class Inner(val result: String)

inline class A(val inner: Inner = Inner("OK"))

fun box(): String {
    return A().inner.result
}
