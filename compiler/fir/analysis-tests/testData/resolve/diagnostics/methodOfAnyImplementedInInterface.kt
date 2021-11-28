interface A {
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override fun toString() = "Hello"<!>
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override fun equals(other: Any?) = true<!>
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override fun hashCode(): Int {
        return 42;
    }<!>
}

interface B {
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface C {
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun toString(): String = "Rest"<!>
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override operator fun equals(other: Any?): Boolean = false<!>
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hashCode(): Int = 2<!>
}

interface D {
    override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun toString(): String
    override operator fun equals(other: Any?): Boolean
    override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hashCode(): Int
}
