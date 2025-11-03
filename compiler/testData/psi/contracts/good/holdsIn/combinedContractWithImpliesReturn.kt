// LANGUAGE: +HoldsInContracts, +ConditionImpliesReturnsContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

inline fun <R> holdsInAndImpliesReturn(condition: Boolean, x: Any?, block: () -> R): String? {
    contract {
        condition holdsIn block
        (x is String) implies returnsNotNull()
    }
    return ""
}
