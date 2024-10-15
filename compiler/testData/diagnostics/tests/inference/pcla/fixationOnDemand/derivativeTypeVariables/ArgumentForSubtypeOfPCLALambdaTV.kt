// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    // ISSUE: KT-71662
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        otvOwner.constrain(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provideBox<!>()) // Box<PNTv> <: OTv

        // ScopeOwner <: PNTv
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(ScopeOwner())

        // expected current state of the constraint system:
        // Box<PNTv> <: OTv
        // ScopeOwner <: PNTv

        // should fix OTv := Box<PNTv> & PNTv := ScopeOwner for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>unbox<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>function<!>()

        // expected: Interloper </: Box<ScopeOwner>
        otvOwner.constrain(Interloper)
    }
    // expected: Box<ScopeOwner>
    <!DEBUG_INFO_EXPRESSION_TYPE("Interloper")!>resultA<!>

    // ISSUE: KT-72030
    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        otvOwner.constrain(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provideBox<!>()) // Box<PNTv> <: OTv

        // Box<ScopeOwner> <: OTv
        otvOwner.constrain(Box<ScopeOwner>())

        // expected current state of the constraint system:
        // Box<ScopeOwner> <: OTv
        //       Box<PNTv> <:

        // should fix PNTv := ScopeOwner for scope navigation
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provide<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>function<!>()

        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
        // expected: Interloper </: ScopeOwner
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(Interloper)
    }
    // expected: CST(Box<ScopeOwner>, Interloper) == kotlin.Any
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>resultB<!>
}


class Box<BXT> {
    fun unbox(): BXT = null!!
}

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun <PNT> createDerivativeTypeVariable(): TypeVariableOwner<PNT> = TypeVariableOwner()
    fun provideBox(): Box<T> = Box()
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
