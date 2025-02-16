// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ConcreteScopeOwner())
        // fixation of OTv is not required
        otvOwner.provide() is ConcreteScopeOwnerSubtype
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ConcreteScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ConcreteScopeOwner())
        // fixation of OTv is not required
        otvOwner.provide() !is ConcreteScopeOwnerSubtype
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(ConcreteScopeOwner, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(GenericScopeOwner<TypeArgument>())
        // fixation of OTv is not required
        otvOwner.provide() is GenericScopeOwnerSubtype<*>
        // expected: Interloper <: OTv
        otvOwner.constrain(Interloper)
    }
    // expected: CST(GenericScopeOwner<TypeArgument>, Interloper) == BaseType
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(GenericScopeOwner<TypeArgument>())
        // should fix OTv := ScopeOwner to acquire type arguments for GSOS type constructor via bare type inference
        otvOwner.provide() is GenericScopeOwnerSubtype
        // expected: Interloper </: GenericScopeOwner<TypeArgument>
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; GenericScopeOwner<TypeArgument>")!>Interloper<!>)
    }
    // expected: GenericScopeOwner<TypeArgument>
    <!DEBUG_INFO_EXPRESSION_TYPE("GenericScopeOwner<TypeArgument>")!>resultD<!>

    // ISSUE: KT-71744
    pcla { otvOwner ->
        otvOwner.constrain(GenericScopeOwner<TypeArgument>())
        // fixation of OTv can ensure bare type inference results for the PTA (from GSOS) <: PTA (from GSO) check
        otvOwner.provide() is <!CANNOT_CHECK_FOR_ERASED!>GenericScopeOwnerSubtype<TypeArgument><!>
        // but if this type constraint source is absent, the check above will succeed anyway
        otvOwner.constrain(Interloper)
        // is there a way to construct a PCLA lambda
        // such that the presence of CANNOT_CHECK_FOR_ERASED
        // will only depend on the presence of PCLA?
    }
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

object Interloper: BaseType
