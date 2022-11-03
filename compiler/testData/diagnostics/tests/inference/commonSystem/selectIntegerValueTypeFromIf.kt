// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun takeLong(i: Long) {}

fun test() {
    takeLong(if (true) 1 else 0)
}