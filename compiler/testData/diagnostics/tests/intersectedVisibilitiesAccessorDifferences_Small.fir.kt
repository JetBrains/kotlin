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

<!VAR_OVERRIDDEN_BY_VAL, VAR_OVERRIDDEN_BY_VAL, VAR_OVERRIDDEN_BY_VAL!>abstract class C1 : WithInternalVar(), IVal<!>
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>C2<!> : WithInternalVar(), IVar
<!VAR_OVERRIDDEN_BY_VAL, VAR_OVERRIDDEN_BY_VAL, VAR_OVERRIDDEN_BY_VAL!>abstract class C3 : IVal, WithInternalVar()<!>
abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, CANNOT_WEAKEN_ACCESS_PRIVILEGE!>C4<!> : IVar, WithInternalVar()
<!VAR_OVERRIDDEN_BY_VAL!>abstract class C5 : WithInternalVar(), IValDefault<!>
abstract class C6 : WithInternalVar(), IVarDefault
<!VAR_OVERRIDDEN_BY_VAL!>abstract class C7 : IValDefault, WithInternalVar()<!>
abstract class C8 : IVarDefault, WithInternalVar()
