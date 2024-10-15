// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        val otvValue = otvOwner.provide()
        if (<!USELESS_IS_CHECK!>otvValue is IntersectionArgument<!>) {
            // expected: IntersectionArgument & ScopeOwner
            <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>otvValue<!>
            // should fix OTv := ScopeOwner for scope navigation
            otvValue.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
        }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    // ISSUE: KT-71867
    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        if (otvOwner === TypeVariableOwner<IntersectionArgument>()) {
            // expected: IntersectionArgument & ScopeOwner
            <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(OT)")!>otvOwner.provide()<!>
            // should fix OTv := ScopeOwner for scope navigation
            otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
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
