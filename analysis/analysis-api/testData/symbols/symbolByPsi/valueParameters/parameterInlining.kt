// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
inline fun foo(
    inlineParameter: () -> Int,
    crossinline crossinlineParameter: () -> Int,
    noinline noinlineParameter: () -> Int,
) {}