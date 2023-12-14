// FIR_IDENTICAL
// ISSUE: KT-60225

class Klass<T: Klass<T>>

fun <T: Klass<T>> Klass<T>.foo() {}

fun main() {
    Klass().foo()
}