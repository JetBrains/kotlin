// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-77986, KT-25341

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

inline fun test2(crossinline onInit: () -> Unit) {
    object {
        fun foo() {
            class Local(val x: Int) {
                init { onInit() }
            }
            Local(1)
        }
    }
}

inline fun test3(block: () -> Unit = { <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> Local }) {
    block()
}
