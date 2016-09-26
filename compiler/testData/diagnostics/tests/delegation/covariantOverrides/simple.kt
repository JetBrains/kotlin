interface IBase1 {
    fun foo(): Any
}

interface IDerived1 : IBase1 {
    override fun foo(): String
}

<!RETURN_TYPE_MISMATCH_BY_DELEGATION!>class Broken1<!>(val b: IBase1) : IBase1 by b, IDerived1

interface IBase2 {
    val foo: Any
}

interface IDerived2 : IBase2 {
    override val foo: String
}

<!PROPERTY_TYPE_MISMATCH_BY_DELEGATION!>class Broken2<!>(val b: IBase2) : IBase2 by b, IDerived2
