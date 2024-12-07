// RUN_PIPELINE_TILL: FRONTEND
fun testA() {
    fun <SOT> nestedPCLA(shallowAnchor: SOT, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultAA = PCLA { fotvOwner ->
        val nestedResultAA = nestedPCLA(fotvOwner.provide()) { sotvOwner ->
            // FOTv <: SOTv

            fotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: FOTv <: SOTv

            sotvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
            // SOTv := FOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(Interloper)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(Interloper)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>nestedResultAA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultAA<!>
}

fun testB() {
    fun <SOT> nestedPCLA(invariantAnchor: Anchor<SOT>, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultBA = PCLA { fotvOwner ->
        val nestedResultBA = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<FOTv> <: Anchor<SOTv>  =>  FOTv == SOTv

            fotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: FOTv == SOTv

            fotvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
            // FOTv := SOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(Interloper)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(Interloper)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>nestedResultBA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultBA<!>

    val resultBB = PCLA { fotvOwner ->
        val nestedResultBB = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<FOTv> <: Anchor<SOTv>  =>  FOTv == SOTv

            sotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: SOTv == FOTv

            sotvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
            // SOTv := FOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(Interloper)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(Interloper)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>nestedResultBB<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultBB<!>
}

fun testC() {
    fun <SOT> nestedPCLA(covariantAnchor: Anchor<out SOT>, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultCA = PCLA { fotvOwner ->
        val nestedResultCA = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<out FOTv> <: Anchor<out SOTv>  =>  FOTv <: SOTv

            fotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: FOTv <: SOTv

            sotvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
            // SOTv := FOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(Interloper)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(Interloper)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>nestedResultCA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultCA<!>
}

fun testD() {
    fun <SOT> nestedPCLA(contravariantAnchor: Anchor<in SOT>, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultDA = PCLA { fotvOwner ->
        val nestedResultDA = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<in FOTv> <: Anchor<in SOTv>  =>  SOTv <: FOTv

            sotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: SOTv <: FOTv

            fotvOwner.provide().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>function<!>()
            // FOTv := SOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(Interloper)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(Interloper)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>nestedResultDA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("BaseType")!>resultDA<!>
}


class Anchor<AT>

class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
    fun provideAnchor(): Anchor<T> = null!!
}

fun <FOT> PCLA(lambda: (TypeVariableOwner<FOT>) -> Any?): FOT = null!!

interface BaseType

class ScopeOwner: BaseType {
    fun function() {}
}

object Interloper: BaseType
