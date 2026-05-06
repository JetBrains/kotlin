// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
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

interface I {
    fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>hashCode<!>(): Int
    context(string: String) fun hashCode(): Int
    fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>toString<!>(): String
    context(string: String) fun toString(): String
}

interface J : I {
    override fun String.hashCode(): Int = 1

    context(string: String)
    override fun hashCode(): Int = 1

    override fun String.toString(): String = ""

    context(string: String)
    override fun toString(): String = ""
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, interfaceDeclaration, nullableType, operator, override,
stringLiteral */
