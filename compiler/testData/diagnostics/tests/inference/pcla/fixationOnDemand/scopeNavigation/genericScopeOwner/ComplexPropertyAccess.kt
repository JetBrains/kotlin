// ISSUE: KT-71662

fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>accessorBackedMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>delegatedMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>
    
    val resultC = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>pcla<!> { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>accessorBackedExtensionProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!TYPE_MISMATCH!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[Error type: Not found recorded type for pcla { otvOwner ->        otvOwner.constrain(ScopeOwner(Value))        // should fix OTv := ScopeOwner<Value> for scope navigation        otvOwner.provide().accessorBackedExtensionProperty        // expected: Interloper </: ScopeOwner<Value>        otvOwner.constrain(Interloper)    }]")!>resultC<!>

    val resultD = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>pcla<!> { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>delegatedExtensionProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!TYPE_MISMATCH!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[Error type: Not found recorded type for pcla { otvOwner ->        otvOwner.constrain(ScopeOwner(Value))        // should fix OTv := ScopeOwner<Value> for scope navigation        otvOwner.provide().delegatedExtensionProperty        // expected: Interloper </: ScopeOwner<Value>        otvOwner.constrain(Interloper)    }]")!>resultD<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>accessorBackedMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType?")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>delegatedMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType?")!>resultB<!>
    
    val resultC = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>pcla<!> { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>?.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>accessorBackedExtensionProperty<!>
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!TYPE_MISMATCH!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[Error type: Not found recorded type for pcla { otvOwner ->        otvOwner.constrain(ScopeOwner.Nullable(Value))        // should fix OTv := ScopeOwner<Value>? for scope navigation        otvOwner.provide()?.accessorBackedExtensionProperty        // expected: Interloper </: ScopeOwner<Value>?        otvOwner.constrain(Interloper)    }]")!>resultC<!>

    val resultD = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>pcla<!> { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>?.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>delegatedExtensionProperty<!>
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!TYPE_MISMATCH!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[Error type: Not found recorded type for pcla { otvOwner ->        otvOwner.constrain(ScopeOwner.Nullable(Value))        // should fix OTv := ScopeOwner<Value>? for scope navigation        otvOwner.provide()?.delegatedExtensionProperty        // expected: Interloper </: ScopeOwner<Value>?        otvOwner.constrain(Interloper)    }]")!>resultD<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner<SOT>(private val value: SOT): BaseType {
    val accessorBackedMemberProperty: SOT
        get() = value

    operator fun getValue(reference: ScopeOwner<SOT>, property: Any?): SOT = value
    val delegatedMemberProperty: SOT by this

    companion object {
        fun <SOT> Nullable(value: SOT): ScopeOwner<SOT>? = null
    }
}

val <SOTA> ScopeOwner<SOTA>.accessorBackedExtensionProperty: SOTA
    get() = accessorBackedMemberProperty

object GenericDelegate {
    operator fun <SOTX> getValue(reference: ScopeOwner<SOTX>, property: Any?): SOTX = reference.accessorBackedMemberProperty
}

val <SOTB> ScopeOwner<SOTB>.delegatedExtensionProperty: SOTB by GenericDelegate

object Interloper: BaseType
