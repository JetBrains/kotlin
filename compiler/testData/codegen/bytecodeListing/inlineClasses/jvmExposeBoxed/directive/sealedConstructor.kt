// WITH_STDLIB
// JVM_EXPOSE_BOXED

@JvmInline value class P(val n: Int)

sealed class Boxed(val p: P?) {
    class Impl(p: P?) : Boxed(p)
}

sealed class Unboxed(val p: P) {
    class Impl(p: P) : Unboxed(p)
}
