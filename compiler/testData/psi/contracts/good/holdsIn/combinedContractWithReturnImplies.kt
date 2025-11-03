// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

inline fun <R> holdsInAndReturnImplies(condition: Boolean, block: () -> R) {
    contract {
        condition holdsIn block
        returns() implies condition
    }
}
