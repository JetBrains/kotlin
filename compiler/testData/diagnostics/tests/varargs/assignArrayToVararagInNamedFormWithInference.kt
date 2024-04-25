// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> id(x: K) = x

fun main() {
    foo(x = id(arrayOf(1)))
}

fun <T> foo(vararg x: T) {}
