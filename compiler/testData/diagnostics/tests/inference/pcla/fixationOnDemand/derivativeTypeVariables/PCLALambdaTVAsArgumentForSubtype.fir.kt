// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    // ISSUE: KT-72030
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        pntvOwner.constrain(otvOwner.provideBox()) // Box<OTv> <: PNTv

        // Box<ScopeOwner> <: PNTv
        pntvOwner.constrain(Box<ScopeOwner>())

        // expected current state of the constraint system:
        // Box<ScopeOwner> <: PNTv
        //        Box<OTv> <:

        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("Interloper")!>resultA<!>

    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        pntvOwner.constrain(otvOwner.provideBox()) // Box<OTv> <: PNTv

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // Box<OTv> <: PNTv
        // ScopeOwner <: OTv

        // should fix PNTv := Box<OTv> & OTv := ScopeOwner for scope navigation
        pntvOwner.provide().unbox().function()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultB<!>
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
