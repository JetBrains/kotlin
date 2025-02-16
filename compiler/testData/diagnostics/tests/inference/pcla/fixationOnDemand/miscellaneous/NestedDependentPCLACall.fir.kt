// RUN_PIPELINE_TILL: FRONTEND
fun testA() {
    fun <SOT> nestedPCLA(shallowAnchor: SOT, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultAA = PCLA { fotvOwner ->
        val nestedResultAA = nestedPCLA(fotvOwner.provide()) { sotvOwner ->
            // FOTv <: SOTv

            fotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: FOTv <: SOTv

            sotvOwner.provide().function()
            // SOTv := FOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; SOT (of fun <SOT> nestedPCLA) & Any & ScopeOwner")!>Interloper<!>)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; FOT (of fun <FOT> PCLA) & Any & ScopeOwner")!>Interloper<!>)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>nestedResultAA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultAA<!>
}

fun testB() {
    fun <SOT> nestedPCLA(invariantAnchor: Anchor<SOT>, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultBA = PCLA { fotvOwner ->
        val nestedResultBA = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<FOTv> <: Anchor<SOTv>  =>  FOTv == SOTv

            fotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: FOTv == SOTv

            fotvOwner.provide().function()
            // FOTv := SOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; SOT (of fun <SOT> nestedPCLA) & Any & ScopeOwner")!>Interloper<!>)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; FOT (of fun <FOT> PCLA) & Any & ScopeOwner")!>Interloper<!>)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>nestedResultBA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultBA<!>

    val resultBB = PCLA { fotvOwner ->
        val nestedResultBB = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<FOTv> <: Anchor<SOTv>  =>  FOTv == SOTv

            sotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: SOTv == FOTv

            sotvOwner.provide().function()
            // SOTv := FOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; SOT (of fun <SOT> nestedPCLA) & Any & ScopeOwner")!>Interloper<!>)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; FOT (of fun <FOT> PCLA) & Any & ScopeOwner")!>Interloper<!>)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>nestedResultBB<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultBB<!>
}

fun testC() {
    fun <SOT> nestedPCLA(covariantAnchor: Anchor<out SOT>, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultCA = PCLA { fotvOwner ->
        val nestedResultCA = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<out FOTv> <: Anchor<out SOTv>  =>  FOTv <: SOTv

            fotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: FOTv <: SOTv

            sotvOwner.provide().function()
            // SOTv := FOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; SOT (of fun <SOT> nestedPCLA) & Any & ScopeOwner")!>Interloper<!>)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; FOT (of fun <FOT> PCLA) & Any & ScopeOwner")!>Interloper<!>)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>nestedResultCA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultCA<!>
}

fun testD() {
    fun <SOT> nestedPCLA(contravariantAnchor: Anchor<in SOT>, lambda: (TypeVariableOwner<SOT>) -> Any?): SOT = null!!

    val resultDA = PCLA { fotvOwner ->
        val nestedResultDA = nestedPCLA(fotvOwner.provideAnchor()) { sotvOwner ->
            // Anchor<in FOTv> <: Anchor<in SOTv>  =>  SOTv <: FOTv

            sotvOwner.constrain(ScopeOwner())
            // ScopeOwner <: SOTv <: FOTv

            fotvOwner.provide().function()
            // FOTv := SOTv := ScopeOwner

            // expected: Interloper </: ScopeOwner
            fotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; SOT (of fun <SOT> nestedPCLA) & Any & ScopeOwner")!>Interloper<!>)
            // expected: Interloper </: ScopeOwner
            sotvOwner.constrain(<!ARGUMENT_TYPE_MISMATCH("Interloper; FOT (of fun <FOT> PCLA) & Any & ScopeOwner")!>Interloper<!>)
        }
        // expected: ScopeOwner
        <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>nestedResultDA<!>
    }
    // expected: ScopeOwner
    <!DEBUG_INFO_EXPRESSION_TYPE("ScopeOwner")!>resultDA<!>
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
