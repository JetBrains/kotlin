// RUN_PIPELINE_TILL: FRONTEND
fun testRegularNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.instance[Index]
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>
}

fun testSafeNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>?) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner?.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner?.instance[Index]
        // expected: Interloper </: ScopeOwner
        otvOwner?.constrain(<!ARGUMENT_TYPE_MISMATCH("ScopeOwner; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    val instance: T = null!!
}

interface BaseType

object Index
object Value

class ScopeOwner: BaseType {
    operator fun get(index: Index): Value = Value
}

object Interloper: BaseType
