// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun coerceToUnit(f: () -> Unit) {}

class Inv<T>

fun <K> builder(block: Inv<K>.() -> Unit): K = TODO()

fun test() {
    coerceToUnit {
        builder {}
    }
}