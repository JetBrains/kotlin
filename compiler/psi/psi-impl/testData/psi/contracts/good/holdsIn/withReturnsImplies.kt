// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

inline fun <R> runIf(condition: Boolean, block: () -> R): R {
    contract { condition holdsIn block }
    return null!!
}
