open class A

interface I

external open class B

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>C<!> : A

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>D<!> : B, I

external interface <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>K<!> : I