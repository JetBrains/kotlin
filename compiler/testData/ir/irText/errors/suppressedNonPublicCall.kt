class C {
    internal fun bar() {}
}

inline fun C.foo() {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    bar()

}
