interface ILeft {
    override fun toString(): String
}

interface IRight {
    override fun toString(): String
}

interface IDiamond : ILeft, IRight {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun toString(): String = "IDiamond"<!>
}