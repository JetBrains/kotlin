// FIR_IDENTICAL
// ISSUE: KT-57095

open class ValueType<T> {
    class Optional<T>: ValueType<T?>()
}

private fun checkType(type: ValueType<out Any?>) {
    type as ValueType.Optional // K1 & K2: ok
}
