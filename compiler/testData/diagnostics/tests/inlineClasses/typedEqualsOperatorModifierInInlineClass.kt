// FIR_IDENTICAL
// WITH_STDLIB
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LANGUAGE: +CustomEqualsInInlineClasses


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
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: IC3<T>) = true
}

@JvmInline
value class IC4<T>(val x: T) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: IC4<String>) = true
}

@JvmInline
value class IC5<T, R>(val x: T) {
    operator fun equals(other: IC5<*, *>) = true
}