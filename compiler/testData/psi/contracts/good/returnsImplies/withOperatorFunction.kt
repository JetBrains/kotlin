// LANGUAGE: +ConditionImpliesReturnsContracts, +AllowContractsOnSomeOperators
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)

import kotlin.contracts.*

operator fun Int?.invoke(): String? {
    contract {
        (this@invoke != null) implies (returnsNotNull())
    }
    return ""
}
