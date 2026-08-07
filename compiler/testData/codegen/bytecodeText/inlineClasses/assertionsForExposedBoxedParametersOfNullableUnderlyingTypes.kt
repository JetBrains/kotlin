// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class AsAny(val a: Any?)

// The underlying type is nullable, so the mangled counterpart gets no assertion,
// but the exposed overload takes a boxed `AsAny`, which is not nullable.
@JvmExposeBoxed
fun nonNull(p: AsAny) {} // assertion in the exposed overload

@JvmExposeBoxed
fun nullable(p: AsAny?) {}

// 0 checkParameterIsNotNull
// 1 checkNotNullParameter
