// !LANGUAGE: +InlineClasses

external inline class <!WRONG_EXTERNAL_DECLARATION!>C(<!EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val a: Int<!>)<!> {
    fun foo()
}

<!WRONG_MODIFIER_TARGET!>inline<!> external enum class <!WRONG_EXTERNAL_DECLARATION!>E<!> {
    A
}
