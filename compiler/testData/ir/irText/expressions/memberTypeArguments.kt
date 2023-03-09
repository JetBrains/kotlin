// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57429

class GenericClass<T>(val value: T) {
    fun withNewValue(newValue: T) = GenericClass(newValue)
}
