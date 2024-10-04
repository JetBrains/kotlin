// ISSUE: KT-72116

fun reproduce() {
    val resultA = <!NEW_INFERENCE_ERROR!>pcla { otvOwner ->
        otvOwner.constrain(null)
        otvOwner.constrain(GenericKlass(TypeArgument()))
        <!NEW_INFERENCE_ERROR!>otvOwner.provide()<!>.<!INAPPLICABLE_CANDIDATE!>fix<!>()
    }<!>
    // expected: GenericKlass<TypeArgument>?
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>resultA<!>
}

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

class GenericKlass<GKT>(arg: GKT)
open class TypeArgument

fun <PH> PH.fix() {}
