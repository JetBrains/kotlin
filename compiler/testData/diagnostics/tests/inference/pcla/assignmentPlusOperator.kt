fun reproduce() {
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("String?; ScopeOwner")!>otvOwner.myVar<!> += ScopeOwner()
        <!BUILDER_INFERENCE_STUB_RECEIVER, VAL_REASSIGNMENT!>otvOwner.myVal<!> += ScopeOwner()
    }
}

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    var myVar: T = null!!
    val myVal: T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

class ScopeOwner {
    operator fun plus(other: ScopeOwner): ScopeOwner = this
}
