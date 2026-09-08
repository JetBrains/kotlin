// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline value class P(val n: Int)

@JvmExposeBoxed
sealed class Boxed(val p: P?) {
    class Impl(p: P?) : Boxed(p)
}

@JvmExposeBoxed
sealed class Unboxed(val p: P) {
    class Impl(p: P) : Unboxed(p)
}
