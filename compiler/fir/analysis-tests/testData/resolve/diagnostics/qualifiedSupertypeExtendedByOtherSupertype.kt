// !LANGUAGE: -QualifiedSupertypeMayBeExtendedByOtherSupertype
interface IBase<T> {
    fun foo() {}
    fun bar() {}
}

typealias AliasedIBase1 = IBase<String>
typealias AliasedIBase = AliasedIBase1

interface IDerived<T> : IBase<T> {
    override fun foo() {}
    fun qux() {}
}

class Test : IDerived<String>, IBase<String> {
    fun test() {
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>IBase<!>>.foo()
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>IBase<!>>.bar()
        super<IDerived>.foo()
        super<IDerived>.bar()
        super<IDerived>.qux()
    }
}

class Test2 : IDerived<String>, AliasedIBase {
    fun test() {
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>IBase<!>>.foo()
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>IBase<!>>.bar()
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>AliasedIBase<!>>.foo()
        super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>AliasedIBase<!>>.bar()
        super<IDerived>.foo()
        super<IDerived>.bar()
        super<IDerived>.qux()
    }
}
