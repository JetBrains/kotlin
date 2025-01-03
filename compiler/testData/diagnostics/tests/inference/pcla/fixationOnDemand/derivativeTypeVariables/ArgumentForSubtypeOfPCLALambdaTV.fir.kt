// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    // ISSUE: KT-71662
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        otvOwner.constrain(pntvOwner.provideBox()) // Box<PNTv> <: OTv

        // ScopeOwner <: PNTv
        pntvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // Box<PNTv> <: OTv
        // ScopeOwner <: PNTv

        // should fix OTv := Box<PNTv> & PNTv := ScopeOwner for scope navigation
        otvOwner.provide().unbox().function()

        // expected: Interloper </: Box<ScopeOwner>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; Box<PNT (of fun <PNT> createDerivativeTypeVariable)> & Box<ScopeOwner>")!>Interloper<!>)
    }
    // expected: Box<ScopeOwner>
    <!DEBUG_INFO_EXPRESSION_TYPE("Box<ScopeOwner>")!>resultA<!>

    // ISSUE: KT-72030
    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        otvOwner.constrain(pntvOwner.provideBox()) // Box<PNTv> <: OTv

        // Box<ScopeOwner> <: OTv
        otvOwner.constrain(Box<ScopeOwner>())

        // expected current state of the constraint system:
        // Box<ScopeOwner> <: OTv
        //       Box<PNTv> <:

        // should fix PNTv := ScopeOwner for scope navigation
        pntvOwner.provide().<!UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
        // expected: Interloper </: ScopeOwner
        pntvOwner.constrain(Interloper)
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
