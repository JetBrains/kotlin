// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

class GenericClass<T>(val value: T) {
    fun withNewValue(newValue: T) = GenericClass(newValue)
}
