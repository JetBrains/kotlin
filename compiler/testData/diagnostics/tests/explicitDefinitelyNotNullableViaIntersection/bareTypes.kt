// FIR_IDENTICAL
// LANGUAGE: +DefinitelyNonNullableTypes

fun main(x: Collection<String>) {
    if (x is List<!SYNTAX!><!> <!SYNTAX!><!SYNTAX!><!>& Any)<!> {}

    val w: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>List<!> & Any = null!!
}
