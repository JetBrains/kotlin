// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        // InvariantContainer<OTv> <: InvariantContainer<PNTv>  =>  OTv == PNTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideContainer())

        // ScopeOwner <: PNTv
        pntvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: PNTv == OTv

        // should fix OTv := PNTv := ScopeOwner for scope navigation
        otvOwner.provide().function()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; PNT (of fun <PNT> createDerivativeTypeVariable) & Any & ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>

    val resultB = pcla { otvOwner ->
        // InvariantContainer<OTv> <: InvariantContainer<PNTv>  =>  OTv == PNTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideContainer())

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: OTv == PNTv

        // should fix PNTv := OTv := ScopeOwner for scope navigation
        pntvOwner.provide().function()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; PNT (of fun <PNT> createDerivativeTypeVariable) & Any & ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultB<!>

    val resultC = pcla { otvOwner ->
        // InvariantContainer<OTv> <: InvariantContainer<PNTv>  =>  OTv == PNTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideContainer())

        // ScopeOwner <: PNTv
        pntvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: PNTv == OTv

        // should fix PNTv := OTv := ScopeOwner for scope navigation
        pntvOwner.provide().function()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; PNT (of fun <PNT> createDerivativeTypeVariable) & Any & ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultC<!>
}


class InvariantContainer<CT>

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun provideContainer(): InvariantContainer<T> = InvariantContainer()
    fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: InvariantContainer<PNT>): TypeVariableOwner<PNT> = TypeVariableOwner()
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
