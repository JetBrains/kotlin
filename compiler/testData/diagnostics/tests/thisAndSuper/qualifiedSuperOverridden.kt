// FIR_IDENTICAL
// LANGUAGE: +QualifiedSupertypeMayBeExtendedByOtherSupertype
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
        super<IBase>.foo()
        super<IBase>.bar()

        super<IDerived1>.foo()
        super<IDerived1>.bar()
        super<IDerived1>.qux()

        super<IDerived2>.foo()
        super<IDerived2>.bar()
    }
}