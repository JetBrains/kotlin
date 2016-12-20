// !DIAGNOSTICS: -DEPRECATION

open class A

interface I

external open class B

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>C<!> : A

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>D<!> : B, I

@native class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>N<!> : A()

external interface <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>K<!> : I

external enum class E {
    X
}

external enum class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>F<!> : I {
    X
}