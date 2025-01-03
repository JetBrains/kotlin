// RUN_PIPELINE_TILL: FRONTEND
fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().accessorBackedProperty = Value
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().delegatedProperty = Value
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultB<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        otvOwner.provide()?.accessorBackedProperty = Value
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner?")!>Interloper<!>)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner?")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        otvOwner.provide()?.delegatedProperty = Value
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner?")!>Interloper<!>)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner?")!>resultB<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner: BaseType {
    companion object {
        fun Nullable(): ScopeOwner? = null
    }
}

var <A> A.accessorBackedProperty: Value
    get() = Value
    set(value) {}

operator fun <X> X.getValue(reference: X, property: Any?): Value = Value
operator fun <Y> Y.setValue(reference: Y, property: Any?, value: Value) {}
var <B> B.delegatedProperty: Value by Any()

object Interloper: BaseType
