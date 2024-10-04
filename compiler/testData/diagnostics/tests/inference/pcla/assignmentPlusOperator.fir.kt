fun reproduce() {
    pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        otvOwner.myVar += ScopeOwner()
        otvOwner.<!VAL_REASSIGNMENT!>myVal<!> += ScopeOwner()
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
