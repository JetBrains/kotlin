// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    // ISSUE: KT-71662
    val resultA = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        pntvOwner.constrainBox(otvOwner.provide()) // OTv <: Box<PNTv>

        // ScopeOwner <: PNTv
        pntvOwner.constrain(ScopeOwner())

        // expected current state of the constraint system:
        // OTv <: Box<PNTv>
        // ScopeOwner <: PNTv

        // should fix OTv := Box<PNTv> & PNTv := ScopeOwner for scope navigation
        otvOwner.provide().unbox().function()

        // expected: Interloper </: Box<ScopeOwner>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; Box<PNT (of fun <PNT> createDerivativeTypeVariable)> & Box<ScopeOwner>")!>Interloper<!>)
    }
    // expected: Box<ScopeOwner>
    <!DEBUG_INFO_EXPRESSION_TYPE("Box<ScopeOwner>")!>resultA<!>

    val resultB = pcla { otvOwner ->
        val pntvOwner = otvOwner.createDerivativeTypeVariable()
        pntvOwner.constrainBox(otvOwner.provide()) // OTv <: Box<PNTv>

        // Box<ScopeOwner> <: OTv
        otvOwner.constrain(Box<ScopeOwner>())

        // expected current state of the constraint system:
        // Box<ScopeOwner> <: OTv <: Box<PNTv>

        // should fix OTv := Box<ScopeOwner> & PNTv := ScopeOwner for scope navigation
        pntvOwner.provide().function()

        // expected: Interloper </: Box<ScopeOwner>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; Box<PNT (of fun <PNT> createDerivativeTypeVariable)> & Box<ScopeOwner>")!>Interloper<!>)
    }
    // expected: Box<ScopeOwner>
    <!DEBUG_INFO_EXPRESSION_TYPE("Box<ScopeOwner>")!>resultB<!>
}


class Box<BXT> {
    fun unbox(): BXT = null!!
}

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun <PNT> createDerivativeTypeVariable(): TypeVariableOwner<PNT> = TypeVariableOwner()
    fun constrainBox(subtypeValue: Box<T>) {}
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
