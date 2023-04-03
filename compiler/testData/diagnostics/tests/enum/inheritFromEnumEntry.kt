// FIR_IDENTICAL
enum class E {
    ENTRY
}

class A : E.<!ENUM_ENTRY_AS_TYPE!>ENTRY<!>
