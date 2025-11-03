// LANGUAGE: +HoldsInContracts, +AllowCheckForErasedTypesInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

inline fun <T> runIfIs(value: Any, block: () -> Unit) {
    contract {
        (value is T) holdsIn block
    }
}
