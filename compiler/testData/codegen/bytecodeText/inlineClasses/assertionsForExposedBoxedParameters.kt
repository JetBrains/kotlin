// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IntWrapper(val i: Int)

// The mangled counterpart takes an `int`, only the exposed overload takes a boxed `IntWrapper`.
@JvmExposeBoxed
fun nonNull(p: IntWrapper) {} // assertion in the exposed overload

@JvmExposeBoxed
fun nullable(p: IntWrapper?) {}

@JvmExposeBoxed
context(_: IntWrapper)
fun IntWrapper.receiver(p: IntWrapper) {} // 3 assertions in the exposed overload

// 0 checkParameterIsNotNull
// 4 checkNotNullParameter
