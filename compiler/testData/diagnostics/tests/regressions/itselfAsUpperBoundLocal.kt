// RUN_PIPELINE_TILL: FRONTEND
fun bar() {
    fun <<!CYCLIC_GENERIC_UPPER_BOUND!>T: T?<!>> foo() {}
    foo()
}
