// FIR_IDENTICAL
// JVM_FILE_NAME: ContractsK2OnlyKt
// FE10_IGNORE
// LANGUAGE: +ConditionImpliesReturnsContracts
// LANGUAGE: +HoldsInContracts

@file:OptIn(ExperimentalContracts::class)
package test

import kotlin.contracts.*

@OptIn(ExperimentalExtendedContracts::class)
fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

@OptIn(ExperimentalExtendedContracts::class)
inline fun <R> runIf(condition: Boolean, block: () -> R): R? {
    contract { condition holdsIn block }
    return if (condition) { block() } else null
}
