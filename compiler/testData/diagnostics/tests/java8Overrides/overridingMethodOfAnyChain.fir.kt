interface IA

interface IB : IA {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun toString(): String = "IB"<!>
}

interface IC : IB {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun toString(): String = "IC"<!>
}

interface ID : IC {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun toString(): String = "ID"<!>
}