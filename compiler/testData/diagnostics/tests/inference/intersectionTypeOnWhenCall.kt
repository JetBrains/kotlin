// FIR_IDENTICAL
// ISSUE: KT-62819

class A<T>

fun foo(cond: Boolean) {
    val first = when (cond) {
        true -> A<Int>()
        false -> A<String?>()
    }

    val second = when (cond) {
        true -> first
        false -> first
    }
}