// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71662

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>InnerClass<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultA<!>
    
    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::accessorBackedReadableMemberProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::accessorBackedWritableMemberProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::delegatedReadableMemberProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultD<!>

    val resultE = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::delegatedWriteableMemberProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultE<!>

    // ISSUE: KT-72031
    val resultF = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::accessorBackedReadableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)> & ScopeOwner<SOTA (of val <SOTA> ScopeOwner<SOTA>.accessorBackedReadableExtensionProperty)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultF<!>

    // ISSUE: KT-72031
    val resultG = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::accessorBackedWriteableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)> & ScopeOwner<SOTB (of var <SOTB> ScopeOwner<SOTB>.accessorBackedWriteableExtensionProperty)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultG<!>

    // ISSUE: KT-72031
    val resultH = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::delegatedReadableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)> & ScopeOwner<SOTC (of val <SOTC> ScopeOwner<SOTC>.delegatedReadableExtensionProperty)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultH<!>

    // ISSUE: KT-72031
    val resultI = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::delegatedWriteableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner<SOT (of class ScopeOwner<SOT>)> & ScopeOwner<SOTD (of var <SOTD> ScopeOwner<SOTD>.delegatedWriteableExtensionProperty)>")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<Value>")!>resultI<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner<SOT>(private val value: SOT): BaseType {
    inner class InnerKlass

    val accessorBackedReadableMemberProperty: SOT
        get() = value

    var accessorBackedWritableMemberProperty: SOT
        get() = value
        set(value) {}

    operator fun getValue(reference: ScopeOwner<SOT>, property: Any?): SOT = value
    operator fun setValue(reference: ScopeOwner<SOT>, property: Any?, value: SOT) {}
    
    val delegatedReadableMemberProperty: SOT by this
    var delegatedWriteableMemberProperty: SOT by this
    
    companion object {
        fun <SOT> Nullable(value: SOT): ScopeOwner<SOT>? = null
    }
}

val <SOTA> ScopeOwner<SOTA>.accessorBackedReadableExtensionProperty: SOTA
    get() = accessorBackedReadableMemberProperty

var <SOTB> ScopeOwner<SOTB>.accessorBackedWriteableExtensionProperty: SOTB
    get() = accessorBackedReadableMemberProperty
    set(value) {}

object GenericDelegate {
    operator fun <SOTX> getValue(reference: ScopeOwner<SOTX>, property: Any?): SOTX = reference.accessorBackedReadableMemberProperty
    operator fun <SOTY> setValue(reference: ScopeOwner<SOTY>, property: Any?, value: SOTY) {}
}

val <SOTC> ScopeOwner<SOTC>.delegatedReadableExtensionProperty: SOTC by GenericDelegate
var <SOTD> ScopeOwner<SOTD>.delegatedWriteableExtensionProperty: SOTD by GenericDelegate

object Interloper: BaseType
