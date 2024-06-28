// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LANGUAGE: +CustomEqualsInValueClasses +ValueClasses

@JvmInline
value class MFVC1(val x: Int, val y: Int) {
    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?): Boolean {
        if (other !is MFVC1) {
            return false
        }
        return x == other.x
    }
}

@JvmInline
value class MFVC2(val x: Int, val y: Int) {
    override fun hashCode() = 0
}

@JvmInline
value class MFVC3(val x: Int, val y: Int) {
    override fun equals(other: Any?) = true

    fun equals(other: MFVC3) = true

    override fun hashCode() = 0
}
