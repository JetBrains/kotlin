// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    val resultA = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide() < ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultA<!>

    val resultB = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide() <= ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultB<!>

    val resultC = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide() > ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultC<!>

    val resultD = pcla { otvOwner ->
        otvOwner.constrain(ScopeOwner())
        // should fix OTv := ScopeOwner for scope navigation
        otvOwner.provide() >= ScopeOwner()
        // expected: Interloper </: ScopeOwner
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; ScopeOwner")!>Interloper<!>)
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultD<!>

    val resultE = pcla { otvOwner ->
        otvOwner.constrain(42.0)
        // should fix OTv := Double for scope navigation
        otvOwner.provide() < 0.0
        // expected: kotlin.Int </: kotlin.Double
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Int; Double")!>42<!>)
    }
    // expected: kotlin.Double
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double")!>resultE<!>

    val resultF = pcla { otvOwner ->
        otvOwner.constrain(42.0f)
        // should fix OTv := Float for scope navigation
        otvOwner.provide() < 0.0
        // expected: kotlin.Int </: kotlin.Float
        otvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Int; Float")!>42<!>)
    }
    // expected: kotlin.Float
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>resultF<!>
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

interface BaseType

class ScopeOwner: BaseType {
    operator fun compareTo(other: ScopeOwner): Int = 0
}

object Interloper: BaseType
