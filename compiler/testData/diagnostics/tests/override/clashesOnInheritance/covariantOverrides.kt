interface IBase {
    fun copy(): IBase
}

interface ILeft : IBase {
    override fun copy(): ILeft
}

open class CLeft : ILeft {
    override fun copy(): ILeft = CLeft()
}

interface IRight : IBase {
    override fun copy(): IRight
}

interface IDerived : ILeft, IRight {
    override fun copy(): IDerived
}

// Error: ILeft::copy and IRight::copy have unrelated return types
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED, RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class CDerivedInvalid1<!> : ILeft, IRight

// Error: CLeft::copy and IRight::copy have unrelated return types
<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class CDerivedInvalid2<!> : CLeft(), IRight

// OK: CDerived1::copy overrides both ILeft::copy and IRight::copy
class CDerived1 : ILeft, IRight {
    override fun copy(): CDerived1 = CDerived1()
}

// Although ILeft::copy and IRight::copy return types are unrelated, IDerived::copy return type is the most specific of three.
abstract class CDerived2 : ILeft, IRight, IDerived

class CDerived2a : ILeft, IRight, IDerived {
    override fun copy(): IDerived = CDerived2a()
}