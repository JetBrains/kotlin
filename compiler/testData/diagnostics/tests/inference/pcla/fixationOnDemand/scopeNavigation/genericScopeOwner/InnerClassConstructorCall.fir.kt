// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71662
// ISSUE: KT-72115

fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().InnerKlass()
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<Value>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultA<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.InnerKlass()
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<Value>?; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>?")!>resultA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner<SOT>(value: SOT): BaseType {
    inner class InnerKlass
    companion object {
        fun <SOT> Nullable(value: SOT): ScopeOwner<SOT>? = null
    }
}

object Interloper: BaseType
