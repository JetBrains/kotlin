// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57095

open class ValueType<T> {
    class Optional<T>: ValueType<T?>()
}

private fun checkType(type: ValueType<out Any?>) {
    type <!UNCHECKED_CAST!>as <!UNSAFE_DOWNCAST_WRT_VARIANCE!>ValueType.Optional<!><!>
}
