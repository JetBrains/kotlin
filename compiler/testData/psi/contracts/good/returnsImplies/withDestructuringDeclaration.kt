@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// LANGUAGE: +AllowContractsOnSomeOperators, +ConditionImpliesReturnsContracts
// ISSUE: KT-79220
import kotlin.contracts.*

class PairList<T>(val items: List<T>)

operator fun <T> PairList<T>?.component1(): T {
    contract {
        (this@component1!= null) implies (returnsNotNull())
    }
    @Suppress("UNCHECKED_CAST")
    return true as T
}
