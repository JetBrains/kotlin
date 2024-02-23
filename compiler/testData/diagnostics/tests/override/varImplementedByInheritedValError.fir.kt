// LANGUAGE: +ProhibitImplementingVarByInheritedVal

interface IVal {
    val a: String
}
interface IVar {
    var a: String
}
interface IVarDefault {
    var a: String
        get() = ""
        set(value) {}
}
open class CVal {
    val a: String = "default"
}
open class CVar {
    var a: String = "default"
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class C1<!> : IVar, IVarDefault
<!VAR_IMPLEMENTED_BY_INHERITED_VAL_ERROR!>class C2<!> : CVal(), IVar
<!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C3<!> : CVal(), IVarDefault
<!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C4<!> : CVal(), IVar, IVarDefault
class C5 : CVar(), IVar
<!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C6<!> : CVar(), IVarDefault
<!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C7<!> : CVar(), IVar, IVarDefault
<!VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION!>class C8<!>(ival: IVal) : IVar, IVal by ival
