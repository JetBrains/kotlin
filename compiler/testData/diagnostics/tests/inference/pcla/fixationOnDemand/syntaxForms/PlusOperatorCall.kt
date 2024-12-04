// RUN_PIPELINE_TILL: FRONTEND
fun testRegularNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("String?; ScopeOwner")!>otvOwner.instance<!> + ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!TYPE_MISMATCH("String?; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
}

fun testSafeNavigation() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>?) -> Unit): OT = null!!

    val resultA = pcla { otvOwner ->
        otvOwner?.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("String?; ScopeOwner")!>otvOwner?.instance<!> + ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner?.constrain(<!TYPE_MISMATCH("String?; Interloper")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    val instance: T = null!!
}

interface BaseType

class ScopeOwner: BaseType {
    operator fun plus(other: ScopeOwner): ScopeOwner = this
}

object Interloper: BaseType
