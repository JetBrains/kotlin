// ISSUE: KT-66717

abstract class WithVal {
    abstract val x: Int
    open val y: Int = 42
    final val z: Int = 42
}
abstract class WithVar {
    abstract var x: Int
    open var y: Int = 42
    final var z: Int = 42
}
abstract class WithProtectedVar {
    abstract protected var x: Int
    open protected var y: Int = 42
    final protected var z: Int = 42
}
abstract class WithInternalVar {
    abstract internal var x: Int
    open internal var y: Int = 42
    final internal var z: Int = 42
}
abstract class WithVarInternalSet {
    abstract var x: Int
        internal set
    open var y: Int = 42
        internal set
    final var z: Int = 42
        internal set
}
abstract class WithVarProtectedSet {
    abstract var x: Int
        protected set
    open var y: Int = 42
        protected set
    final var z: Int = 42
        protected set
}
abstract class WithVarPrivateSet {
    abstract var x: Int
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set
    open var y: Int = 42
        <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private<!> set
    final var z: Int = 42
        private set
}

interface IVal {
    val x: Int
    val y: Int
    val z: Int
}
interface IVar {
    var x: Int
    var y: Int
    var z: Int
}

interface IValDefault {
    val x: Int get() = 42
}
interface IVarDefault {
    var x: Int
        get() = 42
        set(value) {}
}


abstract class A1 : WithVal(), IVal
abstract class B1 : WithVar(), IVal
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>C1<!> : WithProtectedVar(), IVal
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>D1<!> : WithInternalVar(), IVal
abstract class E1 : WithVarInternalSet(), IVal
abstract class F1 : WithVarProtectedSet(), IVal
abstract class G1 : WithVarPrivateSet(), IVal

abstract <!VAR_IMPLEMENTED_BY_INHERITED_VAL_ERROR!>class A2<!> : WithVal(), IVar
abstract class B2 : WithVar(), IVar
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>C2<!> : WithProtectedVar(), IVar
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>D2<!> : WithInternalVar(), IVar
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING, CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>E2<!> : WithVarInternalSet(), IVar
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING, CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>F2<!> : WithVarProtectedSet(), IVar
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING, CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>G2<!> : WithVarPrivateSet(), IVar

abstract class A3 : IVal, WithVal()
abstract class B3 : IVal, WithVar()
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>C3<!> : IVal, WithProtectedVar()
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>D3<!> : IVal, WithInternalVar()
abstract class E3 : IVal, WithVarInternalSet()
abstract class F3 : IVal, WithVarProtectedSet()
abstract class G3 : IVal, WithVarPrivateSet()

abstract <!VAR_IMPLEMENTED_BY_INHERITED_VAL_ERROR!>class A4<!> : IVar, WithVal()
abstract class B4 : IVar, WithVar()
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>C4<!> : IVar, WithProtectedVar()
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>D4<!> : IVar, WithInternalVar()
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING, CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>E4<!> : IVar, WithVarInternalSet()
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING, CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>F4<!> : IVar, WithVarProtectedSet()
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING, CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>G4<!> : IVar, WithVarPrivateSet()

abstract class A5 : WithVal(), IValDefault
abstract class B5 : WithVar(), IValDefault
abstract class C5 : WithProtectedVar(), IValDefault
abstract class D5 : WithInternalVar(), IValDefault
abstract class E5 : WithVarInternalSet(), IValDefault
abstract class F5 : WithVarProtectedSet(), IValDefault
abstract class G5 : WithVarPrivateSet(), IValDefault

abstract class A6 : WithVal(), IVarDefault
abstract class B6 : WithVar(), IVarDefault
abstract class C6 : WithProtectedVar(), IVarDefault
abstract class D6 : WithInternalVar(), IVarDefault
abstract class E6 : WithVarInternalSet(), IVarDefault
abstract class F6 : WithVarProtectedSet(), IVarDefault
abstract class G6 : WithVarPrivateSet(), IVarDefault

abstract class A7 : IValDefault, WithVal()
abstract class B7 : IValDefault, WithVar()
abstract class C7 : IValDefault, WithProtectedVar()
abstract class D7 : IValDefault, WithInternalVar()
abstract class E7 : IValDefault, WithVarInternalSet()
abstract class F7 : IValDefault, WithVarProtectedSet()
abstract class G7 : IValDefault, WithVarPrivateSet()

abstract class A8 : IVarDefault, WithVal()
abstract class B8 : IVarDefault, WithVar()
abstract class C8 : IVarDefault, WithProtectedVar()
abstract class D8 : IVarDefault, WithInternalVar()
abstract class E8 : IVarDefault, WithVarInternalSet()
abstract class F8 : IVarDefault, WithVarProtectedSet()
abstract class G8 : IVarDefault, WithVarPrivateSet()
