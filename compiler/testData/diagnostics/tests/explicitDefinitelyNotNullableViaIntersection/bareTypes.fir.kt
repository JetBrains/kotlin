// !LANGUAGE: +DefinitelyNonNullableTypes

fun main(x: Collection<String>) {
    if (x is List<!SYNTAX!><!> <!SYNTAX!><!SYNTAX!><!>& Any)<!> {}

    val w: List & Any = null!!
}
