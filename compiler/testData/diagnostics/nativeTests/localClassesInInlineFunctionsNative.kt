// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-77986

inline fun test(crossinline onInit: () -> Unit) {
    run {
        <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> Local(val x: Int) {
            init { onInit() }
        }
        Local(1)
    }
}

fun main() {
    test {}
}
