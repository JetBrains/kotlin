interface IBase1 {
    fun foo(): Any
}

open class IDerived1 : IBase1 {
    override fun foo(): String = "1"
}

<!RETURN_TYPE_MISMATCH_BY_DELEGATION, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class Broken1<!>(val b: IBase1) : IBase1 by b, IDerived1()

interface IBase2 {
    val foo: Any
}

open class IDerived2 : IBase2 {
    override val foo: String = "2"
}

<!PROPERTY_TYPE_MISMATCH_BY_DELEGATION, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class Broken2<!>(val b: IBase2) : IBase2 by b, IDerived2()
