// ISSUE: KT-72116

fun reproduce() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(null)
        otvOwner.constrain(GenericKlass(TypeArgument()))
        otvOwner.provide().fix()
    }
    // expected: GenericKlass<TypeArgument>?
    <!DEBUG_INFO_EXPRESSION_TYPE("GenericKlass<TypeArgument>?")!>resultA<!>
}

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

class GenericKlass<GKT>(arg: GKT)
open class TypeArgument

fun <PH> PH.fix() {}
