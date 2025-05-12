// LANGUAGE: +ConditionImpliesReturnsContracts
// LANGUAGE: +HoldsInContracts

package test

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
inline fun <R> runIf(condition: Boolean, block: () -> R): R? {
    contract { condition holdsIn block }
    return if (condition) { block() } else null
}
