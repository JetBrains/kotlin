// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

abstract value class Abstract(<!ABSTRACT_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val x: Int<!>) {
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

sealed value class Sealed(<!SEALED_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val x: Int<!>) {
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

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, companionObject, equalityExpression, functionDeclaration,
isExpression, nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, smartcast,
stringLiteral, thisExpression, value */
