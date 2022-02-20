// !LANGUAGE: +DefinitelyNonNullableTypes

fun main(x: Collection<String>) {
    if (x is List<!SYNTAX!><!> <!SYNTAX!><!SYNTAX!><!>& Any)<!> {}

    val w: <!UNSUPPORTED!><!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>List<!> & Any<!> = null!!
}
