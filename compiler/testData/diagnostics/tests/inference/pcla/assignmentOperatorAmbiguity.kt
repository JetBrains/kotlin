// ISSUE: KT-71753

fun reproduce() {
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("String?; ScopeOwner")!>otvOwner.instance<!> += ScopeOwner()
    }
}

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    var instance: T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

class ScopeOwner {
    operator fun plusAssign(other: ScopeOwner) {}
    operator fun plus(other: ScopeOwner): ScopeOwner = this
}
