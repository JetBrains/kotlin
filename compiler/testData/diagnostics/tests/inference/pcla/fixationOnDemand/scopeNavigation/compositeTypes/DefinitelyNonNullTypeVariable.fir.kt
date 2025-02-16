// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>otvOwner.provide()<!>
        // should fix OTv := ScopeOwner? for scope navigation
        otvOwner.provide().function()
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner?")!>Interloper<!>)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner?")!>resultA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T & Any = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
    companion object {
        fun Nullable(): ScopeOwner? = null
    }
}

object Interloper: BaseType
