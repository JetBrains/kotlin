// RUN_PIPELINE_TILL: FRONTEND
// FILE: test.kt

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // expected: ScopeOwner..ScopeOwner?
        <!DEBUG_INFO_EXPRESSION_TYPE("(TypeVariable(OT)..TypeVariable(OT)?)")!>otvOwner.provide()<!>
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
}


// FILE: TypeVariableOwnerBase.java

public class TypeVariableOwnerBase<JT> {
    public JT provide() { return null; }
}

// FILE: definitionsB.kt

class TypeVariableOwner<KT>: TypeVariableOwnerBase<KT>() {
    fun constrain(subtypeValue: KT) {}
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
