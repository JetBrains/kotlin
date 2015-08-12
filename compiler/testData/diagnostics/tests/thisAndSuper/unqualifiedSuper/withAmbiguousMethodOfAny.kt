interface IA {
    override fun toString(): String = "IA"
}

interface IB {
    override fun toString(): String = "IB"
}

class C : IA, IB {
    override fun toString(): String =
            <!AMBIGUOUS_SUPER!>super<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>toString<!>()
}