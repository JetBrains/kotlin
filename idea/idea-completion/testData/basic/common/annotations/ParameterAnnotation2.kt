// FIR_COMPARISON
annotation class SHello

fun foo(@S<caret>) { }

// INVOCATION_COUNT: 1
// EXIST: SHello
// EXIST: Suppress