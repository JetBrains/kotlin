// FIR_IDENTICAL
// WITH_STDLIB
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

@JvmInline
value class IC1(val x: Int) {
    override fun equals(other: Any?): Boolean {
        return true
    }

    operator fun equals(other: IC1): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return 0
    }
}

@JvmInline
value class IC2(val x: Int) {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: IC1): Boolean {
        return true
    }

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: IC2) {
    }
}