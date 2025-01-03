// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val otvValue = otvOwner.provide()
        if (otvValue is IntersectionArgument) {
            // expected: IntersectionArgument & ScopeOwner
            <!DEBUG_INFO_EXPRESSION_TYPE("IntersectionArgument & ScopeOwner")!>otvValue<!>
            // should fix OTv := ScopeOwner for scope navigation
            otvValue.function()
        }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>

    // ISSUE: KT-71867
    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        if (otvOwner === TypeVariableOwner<IntersectionArgument>()) {
            // expected: IntersectionArgument & ScopeOwner
            <!DEBUG_INFO_EXPRESSION_TYPE("IntersectionArgument & BaseType")!>otvOwner.provide()<!>
            // should fix OTv := ScopeOwner for scope navigation
            otvOwner.provide().<!UNRESOLVED_REFERENCE!>function<!>()
        }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

open class ScopeOwner: BaseType {
    fun function() {}
}

interface IntersectionArgument

object Interloper: BaseType
