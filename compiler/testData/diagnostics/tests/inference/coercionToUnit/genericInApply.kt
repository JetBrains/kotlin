// FIR_IDENTICAL
// WITH_STDLIB

fun <N : Number> addNumber(n: N) = n

fun foo() = 42

fun test() {
    foo().apply(::addNumber)
}
