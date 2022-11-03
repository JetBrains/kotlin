// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@file:Suppress("abc")

fun foo(): Int {
    @Suppress("xyz") return 1
}
