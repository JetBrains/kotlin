interface IBase {
    fun foo() {}
    fun bar() {}
}

interface IDerived1 : IBase {
    override fun foo() {}
    fun qux() {}
}

interface IDerived2 : IBase {
    override fun foo() {}
}

class Test : IDerived1, IBase, IDerived2 {
    override fun foo() {}

    fun test() {
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>IBase<!>>.foo()
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>IBase<!>>.bar()

        super<IDerived1>.foo()
        super<IDerived1>.bar()
        super<IDerived1>.qux()

        super<IDerived2>.foo()
        super<IDerived2>.bar()
    }
}