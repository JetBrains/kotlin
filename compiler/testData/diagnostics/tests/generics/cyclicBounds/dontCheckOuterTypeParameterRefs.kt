// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND

open class A<T : <!CYCLIC_GENERIC_UPPER_BOUND!>S<!>, S : <!CYCLIC_GENERIC_UPPER_BOUND!>T<!>> {
    val x = object {
    }
}
