// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        // ContravariantContainer<OTv> <: ContravariantContainer<PNTv>  =>  PNTv <: OTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideContainer())

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
        // ContravariantContainer<OTv> <: ContravariantContainer<PNTv>  =>  PNTv <: OTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideContainer())

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
    
    val resultC = pcla { otvOwner ->
        // InvariantContainer<in OTv> <: InvariantContainer<in PNTv>  =>  CapturedType(in OTv) <: CapturedType(in PNTv)  =>  PNTv <: OTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideProjectedContainer())

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
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultC<!>

    // ISSUE: KT-72030
    val resultD = pcla { otvOwner ->
        // InvariantContainer<in OTv> <: InvariantContainer<in PNTv>  =>  CapturedType(in OTv) <: CapturedType(in PNTv)  =>  PNTv <: OTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideProjectedContainer())

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
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultD<!>
}


class ContravariantContainer<in CT>
class InvariantContainer<CT>

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun provideContainer(): ContravariantContainer<T> = ContravariantContainer()
    fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: ContravariantContainer<PNT>): TypeVariableOwner<PNT> = TypeVariableOwner()
    fun provideProjectedContainer(): InvariantContainer<in T> = InvariantContainer()
    fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: InvariantContainer<in PNT>): TypeVariableOwner<PNT> = TypeVariableOwner()
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
