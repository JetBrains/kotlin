// IGNORE_LEAKED_INTERNAL_TYPES: KT-72031
// ISSUE: KT-71662

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>memberFunction<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    // ISSUE: KT-72031
    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>::extensionFunction
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOTA>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<TypeVariable(SOTA)>")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>fieldBackedReadableProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>fieldBackedWritableProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultD<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner<SOT>(value: SOT): BaseType {
    fun memberFunction() {}
    val fieldBackedReadableProperty: SOT = value
    var fieldBackedWritableProperty: SOT = value
    companion object {
        fun <SOT> Nullable(value: SOT): ScopeOwner<SOT>? = null
    }
}

fun <SOTA> ScopeOwner<SOTA>.extensionFunction() {}

object Interloper: BaseType
