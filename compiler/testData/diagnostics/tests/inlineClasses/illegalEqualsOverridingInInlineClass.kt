// FIR_IDENTICAL
// WITH_STDLIB
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

@JvmInline
value class IC1(val x: Int) {
    <!ILLEGAL_EQUALS_OVERRIDING_IN_INLINE_CLASS!>override fun equals(other: Any?): Boolean<!> {
        if (other !is IC1) {
            return false
        }
        return x == other.x
    }
}

@JvmInline
value class IC2(val x: Int) {
    override fun hashCode(): Int {
        return 0
    }
}

@JvmInline
value class IC3(val x: Int) {
    override fun equals(other: Any?): Boolean {
        return true
    }

    fun equals(other: IC3): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return 0
    }
}