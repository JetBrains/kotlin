// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun testA() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>, OT) -> Any?): OT = null!!

    val resultAA = pcla { otvOwner, otvValue ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvValue.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultAA<!>
}

fun testB() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> (OT) -> Any?): OT = null!!

    val resultBA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        if (false) return@pcla { otvValue -> otvValue.<!UNRESOLVED_REFERENCE!>function<!>() }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(Interloper)
        return@pcla { _ ->  }
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultBA<!>
}

fun testC() {
    fun <
        FOT,
        SOT: FOT,
        TOT: SOT,
    > pcla(
        lambda: (TypeVariableOwner<SOT>) -> Any?,
    ): Triple<FOT, SOT, TOT> = null!!

    val resultCA = pcla { sotvOwner ->
        sotvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        sotvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
        // expected: Interloper </: ScopeOwner
        sotvOwner.constrain(Interloper)
    }
    // expected: kotlin.Triple<ScopeOwner, ScopeOwner, ScopeOwner>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Triple<BaseType, BaseType, BaseType>")!>resultCA<!>
}

fun testD() {
    fun <
        OT,
    > pcla(
        lambdaA: (TypeVariableOwner<OT>) -> Any?,
        lambdaB: (TypeVariableOwner<OT>) -> Any?,
        lambdaC: (TypeVariableOwner<OT>) -> Any?,
    ): OT = null!!

    val resultDA = pcla(
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ otvOwnerA -> otvOwnerA.constrain(ScopeOwner()) }<!>,
        // should fix OTv := ScopeOwner for scope navigation
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ otvOwnerB -> otvOwnerB.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>() }<!>,
        // expected: Interloper </: ScopeOwner
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ otvOwnerC -> otvOwnerC.constrain(Interloper) }<!>,
    )
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultDA<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
