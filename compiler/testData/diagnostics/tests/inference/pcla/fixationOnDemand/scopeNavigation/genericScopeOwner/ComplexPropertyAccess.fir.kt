// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71662

fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().accessorBackedMemberProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOT (of class ScopeOwner<SOT>)>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().delegatedMemberProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOT (of class ScopeOwner<SOT>)>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultB<!>
    
    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().accessorBackedExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOT (of class ScopeOwner<SOT>)> & ScopeOwner<SOTA (of val <SOTA> ScopeOwner<SOTA>.accessorBackedExtensionProperty)>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide().delegatedExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOT (of class ScopeOwner<SOT>)> & ScopeOwner<SOTB (of val <SOTB> ScopeOwner<SOTB>.delegatedExtensionProperty)>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultD<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.accessorBackedMemberProperty
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOT (of fun <SOT> Nullable)>?; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>?")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.delegatedMemberProperty
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOT (of fun <SOT> Nullable)>?; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>?")!>resultB<!>
    
    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.accessorBackedExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("kotlin.Nothing?; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>?")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable(Value))
        // should fix OTv := ScopeOwner<Value>? for scope navigation
        otvOwner.provide()?.delegatedExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("kotlin.Nothing?; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>?")!>resultD<!>
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
