// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57095

open class ValueType<T> {
    class Optional<T>: ValueType<T?>()
}
private fun checkType(type: ValueType<*>) {
    type as <!DEPRECATED_BARE_DOWNCAST!>ValueType.Optional<!> // K1: ok, K2: NO_TYPE_ARGUMENTS_ON_RHS
}
