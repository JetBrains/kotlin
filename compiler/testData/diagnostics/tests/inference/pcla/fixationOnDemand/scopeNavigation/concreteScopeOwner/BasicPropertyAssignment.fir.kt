// RUN_PIPELINE_TILL: FRONTEND
fun testStandardNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().fieldBackedProperty = Value
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>
}

fun testSafeNavigation() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner.Nullable())
        // should fix OTv := ScopeOwner? for scope navigation
        otvOwner.provide()?.fieldBackedProperty = Value
        // expected: Interloper </: ScopeOwner?
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner?")!>Interloper<!>)
    }
    // expected: ScopeOwner?
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner?")!>resultA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

object Value

class ScopeOwner: BaseType {
    var fieldBackedProperty: Value = Value
    companion object {
        fun Nullable(): ScopeOwner? = null
    }
}

object Interloper: BaseType
