// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        otvOwner.constrain(pntvOwner.provide()) // PNTv <: OTv

        // ScopeOwner <: PNTv
        pntvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: PNTv <: OTv

        // should fix OTv := PNTv := ScopeOwner for scope navigation
        otvOwner.provide().function()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; PNT (of fun <PNT> createDerivativeTypeVariable) & Any & ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>

    // ISSUE: KT-72030
    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        otvOwner.constrain(pntvOwner.provide()) // PNTv <: OTv

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: OTv
        //       PNTv <:

        // should fix PNTv := ScopeOwner for scope navigation
        pntvOwner.provide().<!UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
        // expected: Interloper </: ScopeOwner
        pntvOwner.constrain(Interloper)
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
