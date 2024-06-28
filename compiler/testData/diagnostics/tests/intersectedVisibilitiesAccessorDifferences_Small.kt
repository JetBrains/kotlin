// ISSUE: KT-66717

abstract class WithInternalVar {
    internal abstract var x: Int
    internal open var y: Int = 42
    internal var z: Int = 42
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

abstract class <!CANNOT_INFER_VISIBILITY, CANNOT_INFER_VISIBILITY!>C1<!> : WithInternalVar(), IVal
abstract class <!CANNOT_INFER_VISIBILITY, CANNOT_INFER_VISIBILITY!>C2<!> : WithInternalVar(), IVar
abstract class <!CANNOT_INFER_VISIBILITY, CANNOT_INFER_VISIBILITY!>C3<!> : IVal, WithInternalVar()
abstract class <!CANNOT_INFER_VISIBILITY, CANNOT_INFER_VISIBILITY!>C4<!> : IVar, WithInternalVar()
abstract class C5 : WithInternalVar(), IValDefault
abstract class C6 : WithInternalVar(), IVarDefault
abstract class C7 : IValDefault, WithInternalVar()
abstract class C8 : IVarDefault, WithInternalVar()
