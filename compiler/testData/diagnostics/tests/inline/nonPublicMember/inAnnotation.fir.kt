// ISSUE: KT-60604

private const val MESSAGE = "This is deprecated"

@Deprecated(<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>MESSAGE<!>)
inline fun hello(f: () -> Int): Int = f()
