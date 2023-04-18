// FIR_IDENTICAL

class GenericClass<T>(val value: T) {
    fun withNewValue(newValue: T) = GenericClass(newValue)
}
