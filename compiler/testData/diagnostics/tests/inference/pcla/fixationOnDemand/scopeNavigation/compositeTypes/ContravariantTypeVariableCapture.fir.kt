// FIR_IDENTICAL
// ISSUE: KT-71844

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // expected: CapturedType(in ScopeOwner)
        <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(in BaseType)")!>otvOwner.provide()<!>
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!UNRESOLVED_REFERENCE!>function<!>()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
}


class TypeVariableOwner<T, CT: T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): CT = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT, in OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
