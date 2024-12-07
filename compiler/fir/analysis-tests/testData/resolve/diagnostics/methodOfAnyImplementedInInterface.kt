// RUN_PIPELINE_TILL: FRONTEND
interface A {
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>() = "Hello"
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>equals<!>(other: Any?) = true
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>hashCode<!>(): Int {
        return 42;
    }
}

interface B {
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface C {
    override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): String = "Rest"
    override operator fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>equals<!>(other: Any?): Boolean = false
    override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>hashCode<!>(): Int = 2
}

interface D {
    override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun toString(): String
    override operator fun equals(other: Any?): Boolean
    override <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hashCode(): Int
}
