// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -SENSELESS_COMPARISON

fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // fixation of OTv is not required
        otvOwner.provide() === ScopeOwner()
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // fixation of OTv is not required
        otvOwner.provide() !== ScopeOwner()
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // fixation of OTv is not required
        otvOwner.provide() === null
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // fixation of OTv is not required
        null === otvOwner.provide()
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultD<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType

object Interloper: BaseType
