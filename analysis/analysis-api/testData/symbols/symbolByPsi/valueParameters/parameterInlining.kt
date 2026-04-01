inline fun foo(
    inlineParameter: () -> Int,
    crossinline crossinlineParameter: () -> Int,
    noinline noinlineParameter: () -> Int,
) {}