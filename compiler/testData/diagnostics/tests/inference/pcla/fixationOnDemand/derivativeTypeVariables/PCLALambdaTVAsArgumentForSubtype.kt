// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    // ISSUE: KT-72030
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(otvOwner.provideBox()) // Box<OTv> <: PNTv

        // Box<ScopeOwner> <: PNTv
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(Box<ScopeOwner>())

        // expected current state of the constraint system:
        // Box<ScopeOwner> <: PNTv
        //        Box<OTv> <:

        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("Interloper")!>resultA<!>

    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>createDerivativeTypeVariable<!>()
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>constrain<!>(otvOwner.provideBox()) // Box<OTv> <: PNTv

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // Box<OTv> <: PNTv
        // ScopeOwner <: OTv

        // should fix PNTv := Box<OTv> & OTv := ScopeOwner for scope navigation
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pntvOwner<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>provide<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>unbox<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>
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
