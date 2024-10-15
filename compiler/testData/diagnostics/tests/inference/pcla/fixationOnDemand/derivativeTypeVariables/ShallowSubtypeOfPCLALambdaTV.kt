// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        otvOwner.constrain(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provide<!>()) // PNTv <: OTv

        // ScopeOwner <: PNTv
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: PNTv <: OTv

        // should fix OTv := PNTv := ScopeOwner for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("Interloper")!>resultA<!>

    // ISSUE: KT-72030
    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        otvOwner.constrain(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provide<!>()) // PNTv <: OTv

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: OTv
        //       PNTv <:

        // should fix PNTv := ScopeOwner for scope navigation
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provide<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>function<!>()

        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
        // expected: Interloper </: ScopeOwner
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
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
