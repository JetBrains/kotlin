enum class E {
    ENTRY
}

class A : <!SINGLETON_IN_SUPERTYPE!>E.ENTRY<!>
