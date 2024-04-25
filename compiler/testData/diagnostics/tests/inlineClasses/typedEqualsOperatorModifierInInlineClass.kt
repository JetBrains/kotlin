// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LANGUAGE: +CustomEqualsInValueClasses
// SKIP_TXT


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
