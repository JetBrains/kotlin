// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LANGUAGE: +CustomEqualsInValueClasses, +ValueClasses


@JvmInline
value class MFVC1(val x: Int, val y: Int) {
    override fun equals(other: Any?) = true

    operator fun equals(other: MFVC1) = true

    override fun hashCode() = 0
}

@JvmInline
value class MFVC2(val x: Int, val y: Int) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: MFVC1) = true

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: MFVC2) {
    }
}
