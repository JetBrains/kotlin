// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun f(p: (Int) -> (String) -> Unit) {}

fun g(cond: Boolean) {
    f(if (cond) { i -> { } } else { i -> { } })
}