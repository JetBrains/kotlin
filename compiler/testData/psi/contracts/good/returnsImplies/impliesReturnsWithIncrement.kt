@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// LANGUAGE: +AllowContractsOnSomeOperators, +ConditionImpliesReturnsContracts
// ISSUE: KT-79355

import kotlin.contracts.*

operator fun Any?.inc(): Int? {
    contract {
        returns() implies (this@inc != null)
        (this@inc != null) implies returnsNotNull()
    }
    return (this as Int) + 1
}
