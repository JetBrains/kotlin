// FIR_IDENTICAL
// ISSUE: KT-60604

private const val MESSAGE = "This is deprecated"

@Deprecated(MESSAGE)
inline fun hello(f: () -> Int): Int = f()
