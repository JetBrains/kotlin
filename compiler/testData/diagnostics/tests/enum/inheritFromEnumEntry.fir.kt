enum class E {
    ENTRY
}

class A : <!ENUM_ENTRY_AS_TYPE!>E.ENTRY<!>
