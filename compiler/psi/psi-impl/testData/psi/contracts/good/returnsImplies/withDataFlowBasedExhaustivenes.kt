// LANGUAGE: +ConditionImpliesReturnsContracts, +DataFlowBasedExhaustiveness
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// ISSUE: KT-79271
import kotlin.contracts.*

sealed interface Variants {
    data object A : Variants
    data object B : Variants
    fun foo(){}
}

fun ensureA(v: Variants): Variants? {
    contract {
        (v is Variants.A) implies (returnsNotNull())
    }
    return v as Variants.A
}
