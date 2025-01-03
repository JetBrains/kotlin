// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun testA() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>, OT) -> Any?): OT = null!!

    val resultAA = pcla { otvOwner, otvValue ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvValue.function()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultAA<!>
}

fun testB() {
    fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> (OT) -> Any?): OT = null!!

    val resultBA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        if (false) return@pcla { otvValue -> otvValue.function() }
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
        return@pcla { _ ->  }
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultBA<!>
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
        sotvOwner.provide().function()
        // expected: Interloper </: ScopeOwner
        sotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; FOT (of fun <FOT, SOT : FOT, TOT : SOT> pcla) & Any & TOT (of fun <FOT, SOT : FOT, TOT : SOT> pcla) & Any & ScopeOwner")!>Interloper<!>)
    }
    // expected: kotlin.Triple<ScopeOwner, ScopeOwner, ScopeOwner>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Triple<ScopeOwner, ScopeOwner, ScopeOwner>")!>resultCA<!>
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
        { otvOwnerA -> otvOwnerA.constrain(ScopeOwner()) },
        // should fix OTv := ScopeOwner for scope navigation
        { otvOwnerB -> otvOwnerB.provide().function() },
        // expected: Interloper </: ScopeOwner
        { otvOwnerC -> otvOwnerC.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>) },
    )
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultDA<!>
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
