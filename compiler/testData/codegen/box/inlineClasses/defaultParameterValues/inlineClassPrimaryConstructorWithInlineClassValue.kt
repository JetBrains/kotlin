// WITH_RUNTIME

@JvmInline
value class Inner(val result: String)

@JvmInline
value class A(val inner: Inner = Inner("OK"))

fun box(): String {
    return A().inner.result
}
