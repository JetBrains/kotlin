enum class E {
    ENTRY
}

class A : <!OTHER_ERROR!>E.ENTRY<!>
