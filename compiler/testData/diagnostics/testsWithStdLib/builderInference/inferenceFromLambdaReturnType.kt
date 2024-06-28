// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-57873

class ThemeKey<T>

fun <S> getWithFallback(fallback: (ThemeKey<S>) -> S) {}

fun main() {
    getWithFallback { "" }
}
