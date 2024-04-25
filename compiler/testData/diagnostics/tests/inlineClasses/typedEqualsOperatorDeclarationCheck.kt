// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LANGUAGE: +CustomEqualsInValueClasses


@JvmInline
value class IC1(val x: Int) {
    override fun equals(other: Any?) = true

    operator fun equals(other: IC1) = true

    override fun hashCode() = 0
}

@JvmInline
value class IC2(val x: Int) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: IC1) = true

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: IC2) {
    }
}

@JvmInline
value class IC3<T>(val x: T) {
    operator fun equals(other: <!TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS!>IC3<T><!>) = true
}

@JvmInline
value class IC4<T>(val x: T) {
    operator fun equals(other: <!TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS!>IC4<String><!>) = true
}

@JvmInline
value class IC5<T: Number>(val x: T) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: T) = true
}

@JvmInline
value class IC6<T, R>(val x: T) {
    operator fun<!TYPE_PARAMETERS_NOT_ALLOWED!><S1, S2><!> equals(other: IC6<*, *>) = true
}

@JvmInline
value class IC7<T, R>(val x: T) {
    operator fun equals(other: IC7<*, *>) = true
}

@JvmInline
value class IC8<T, R>(val x: T) {
    operator fun equals(other: IC8<*, *>): Nothing = TODO()
}
