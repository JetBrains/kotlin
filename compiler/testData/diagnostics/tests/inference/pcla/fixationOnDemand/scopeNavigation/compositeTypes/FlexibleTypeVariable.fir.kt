// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

// FILE: test.kt

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // expected: ScopeOwner..ScopeOwner?
        <!DEBUG_INFO_EXPRESSION_TYPE("(ScopeOwner..ScopeOwner?)")!>otvOwner.provide()<!>
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().function()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>
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
