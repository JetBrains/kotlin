// ISSUE: KT-66717

// Cases: {from class, from interface} x {val, var} x {abstract, with implementation}

abstract class ClassWithAbstractVal {
    internal abstract val x: Int
}

abstract class ClassWithAbstractVar {
    internal abstract var x: Int
}

abstract class ClassWithDefaultVal {
    internal val x: Int get() = 10
}

abstract class ClassWithDefaultVar {
    internal var x: Int
        get() = 10
        set(value) {}
}

interface InterfaceWithAbstractVal {
    val x: Int
}

interface InterfaceWithAbstractVar {
    var x: Int
}

interface InterfaceWithDefaultVal {
    val x: Int get() = 10
}

interface InterfaceWithDefaultVar {
    var x: Int
        get() = 10
        set(value) {}
}

abstract class CA1 : ClassWithAbstractVal(), InterfaceWithAbstractVal
abstract class CA2 : ClassWithAbstractVal(), InterfaceWithAbstractVar
abstract class CA3 : ClassWithAbstractVal(), InterfaceWithDefaultVal
abstract class CA4 : ClassWithAbstractVal(), InterfaceWithDefaultVar

abstract class CB1 : ClassWithAbstractVar(), InterfaceWithAbstractVal
abstract class CB2 : ClassWithAbstractVar(), InterfaceWithAbstractVar
abstract class CB3 : ClassWithAbstractVar(), InterfaceWithDefaultVal
abstract class CB4 : ClassWithAbstractVar(), InterfaceWithDefaultVar

abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>CC1<!> : ClassWithDefaultVal(), InterfaceWithAbstractVal
abstract <!VAR_IMPLEMENTED_BY_INHERITED_VAL_ERROR!>class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>CC2<!><!> : ClassWithDefaultVal(), InterfaceWithAbstractVar
abstract <!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class CC3<!> : ClassWithDefaultVal(), InterfaceWithDefaultVal
abstract <!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class CC4<!> : ClassWithDefaultVal(), InterfaceWithDefaultVar

abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>CD1<!> : ClassWithDefaultVar(), InterfaceWithAbstractVal
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>CD2<!> : ClassWithDefaultVar(), InterfaceWithAbstractVar
abstract <!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class CD3<!> : ClassWithDefaultVar(), InterfaceWithDefaultVal
abstract <!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class CD4<!> : ClassWithDefaultVar(), InterfaceWithDefaultVar
