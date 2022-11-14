open class A

interface I

external open class B

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>C<!> : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>A<!>

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>D<!> : <!EXTERNAL_DELEGATED_CONSTRUCTOR_CALL!>B<!>, I

external interface <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>K<!> : I

external enum class E {
    X
}

external enum class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>F<!> : I {
    X
}
