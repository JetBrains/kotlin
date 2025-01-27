// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57095

open class ValueType<T> {
    class Optional<T>: ValueType<T?>()
}

private fun checkType(type: ValueType<out Any?>) {
    type as ValueType.Optional
}
