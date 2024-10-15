// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    // ISSUE: KT-72030
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(otvOwner.provide()) // OTv <: PNTv

        // ScopeOwner <: PNTv
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: PNTv
        //        OTv <:

        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("Interloper")!>resultA<!>

    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(otvOwner.provide()) // OTv <: PNTv

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: OTv <: PNTv

        // should fix PNTv := OTv := ScopeOwner for scope navigation
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provide<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun <PNT> createDerivativeTypeVariable(): TypeVariableOwner<PNT> = TypeVariableOwner()
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
