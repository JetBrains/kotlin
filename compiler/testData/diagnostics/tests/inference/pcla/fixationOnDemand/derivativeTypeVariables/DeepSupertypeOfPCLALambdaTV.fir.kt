// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    // ISSUE: KT-72030
    val resultA = pcla { otvOwner ->
        // CovariantContainer<OTv> <: CovariantContainer<PNTv>  =>  OTv <: PNTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideContainer())

        // ScopeOwner <: PNTv
        pntvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: PNTv
        //        OTv <:

        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("Interloper")!>resultA<!>

    val resultB = pcla { otvOwner ->
        // CovariantContainer<OTv> <: CovariantContainer<PNTv>  =>  OTv <: PNTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideContainer())

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: OTv <: PNTv

        // should fix PNTv := OTv := ScopeOwner for scope navigation
        pntvOwner.provide().function()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; PNT (of fun <PNT> createDerivativeTypeVariable) & Any & ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultB<!>

    // ISSUE: KT-72030
    val resultC = pcla { otvOwner ->
        // InvariantContainer<out OTv> <: InvariantContainer<out PNTv>  =>  CapturedType(out OTv) <: CapturedType(out PNTv)  =>  OTv <: PNTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideProjectedContainer())

        // ScopeOwner <: PNTv
        pntvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: PNTv
        //        OTv <:

        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide().<!UNRESOLVED_REFERENCE!>function<!>()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("Interloper")!>resultC<!>

    val resultD = pcla { otvOwner ->
        // InvariantContainer<out OTv> <: InvariantContainer<out PNTv>  =>  CapturedType(out OTv) <: CapturedType(out PNTv)  =>  OTv <: PNTv
        val pntvOwner = otvOwner.createDerivativeTypeVariable(otvOwner.provideProjectedContainer())

        // ScopeOwner <: OTv
        otvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // ScopeOwner <: OTv <: PNTv

        // should fix PNTv := OTv := ScopeOwner for scope navigation
        pntvOwner.provide().function()

        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; PNT (of fun <PNT> createDerivativeTypeVariable) & Any & ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultD<!>
}


class CovariantContainer<out CT>
class InvariantContainer<CT>

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun provideContainer(): CovariantContainer<T> = CovariantContainer()
    fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: CovariantContainer<PNT>): TypeVariableOwner<PNT> = TypeVariableOwner()
    fun provideProjectedContainer(): InvariantContainer<out T> = InvariantContainer()
    fun <PNT> createDerivativeTypeVariable(constrainingTypeValue: InvariantContainer<out PNT>): TypeVariableOwner<PNT> = TypeVariableOwner()
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
