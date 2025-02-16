// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ConcreteScopeOwner())
        // fixation of OTv is not required
        when (otvOwner.provide()) { is ConcreteScopeOwnerSubtype -> Unit }
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ConcreteScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ConcreteScopeOwner())
        // fixation of OTv is not required
        when (otvOwner.provide()) { !is ConcreteScopeOwnerSubtype -> Unit }
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ConcreteScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(GenericScopeOwner<TypeArgument>())
        // fixation of OTv is not required
        when (otvOwner.provide()) { is GenericScopeOwnerSubtype<*> -> Unit }
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(GenericScopeOwner<TypeArgument>, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(GenericScopeOwner<TypeArgument>())
        // should fix OTv := ScopeOwner to acquire type arguments for GSOS type constructor via bare type inference
        when (otvOwner.provide()) { is GenericScopeOwnerSubtype -> Unit }
        // expected: Interloper </: GenericScopeOwner<TypeArgument>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; GenericScopeOwner<TypeArgument>")!>Interloper<!>)
    }
    // expected: GenericScopeOwner<TypeArgument>
    <!DEBUG_INFO_EXPRESSION_TYPE("GenericScopeOwner<TypeArgument>")!>resultD<!>

    val resultE = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        when (Value) { in otvOwner.provide() -> Unit }
        // expected: Interloper <: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultE<!>

    val resultF = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        when (Value) { !in otvOwner.provide() -> Unit }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultF<!>

    val resultG = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // fixation of OTv is not required
        when (otvOwner.provide()) { ScopeOwner() -> Unit }
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultG<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

open class ConcreteScopeOwner: BaseType
open class ConcreteScopeOwnerSubtype: ConcreteScopeOwner()

open class GenericScopeOwner<A>: BaseType
open class GenericScopeOwnerSubtype<B>: GenericScopeOwner<B>()

interface TypeArgument

object Value

class ScopeOwner: BaseType {
    operator fun contains(value: Value): Boolean = true
}

object Interloper: BaseType
