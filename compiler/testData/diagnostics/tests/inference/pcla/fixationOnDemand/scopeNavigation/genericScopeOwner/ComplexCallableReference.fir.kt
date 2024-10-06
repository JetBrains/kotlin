// IGNORE_LEAKED_INTERNAL_TYPES: KT-72031
// ISSUE: KT-71662

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>InnerClass<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
    
    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>accessorBackedReadableMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>accessorBackedWritableMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>delegatedReadableMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultD<!>

    val resultE = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        otvOwner.provide()::<!UNRESOLVED_REFERENCE!>delegatedWriteableMemberProperty<!>
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultE<!>

    // ISSUE: KT-72031
    val resultF = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>::accessorBackedReadableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOTA>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<TypeVariable(SOTA)>")!>resultF<!>

    // ISSUE: KT-72031
    val resultG = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>::accessorBackedWriteableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOTB>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<TypeVariable(SOTB)>")!>resultG<!>

    // ISSUE: KT-72031
    val resultH = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>::delegatedReadableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOTC>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<TypeVariable(SOTC)>")!>resultH<!>

    // ISSUE: KT-72031
    val resultI = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner(Value))
        // should fix OTv := ScopeOwner<Value> for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>::delegatedWriteableExtensionProperty
        // expected: Interloper </: ScopeOwner<Value>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner<SOTD>; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner<Value>
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner<TypeVariable(SOTD)>")!>resultI<!>
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
