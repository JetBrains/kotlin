// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// RUN_PIPELINE_TILL: FRONTEND
// JVM_TARGET: 23
// ENABLE_JVM_PREVIEW

value class A(val x: Int) {
    override fun equals(other: Any?) = other is A && this.x == other.x
    override fun toString() = "[$x]"
    override fun hashCode() = x.hashCode()
    fun `unbox-impl`() = x
    fun unbox() = x
    companion object {
        fun `box-impl`(x: Int) = A(x)
        fun box(x: Int) = A(x)
    }
}

<!INLINE_CLASS_DEPRECATED!>inline<!> class B(val x: Int) {
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>equals<!>(other: Any?) = other is B && this.x == other.x
    override fun toString() = "[$x]"
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>hashCode<!>() = x.hashCode()
    fun `unbox-impl`() = x
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() = x
    companion object {
        fun `box-impl`(x: Int) = B(x)
        fun box(x: Int) = B(x)
    }
}
