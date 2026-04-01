// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)

import kotlin.contracts.*

inline fun <R> runIf(condition: Boolean, block: () -> R): R? {
    contract { condition holdsIn block }
    return if (condition) { block() } else null
}

inline fun <R> runIfNot(condition: Boolean, block: () -> R): R? {
    contract {
        !condition holdsIn block
    }
    return if (!condition) block() else null
}

inline fun <R> runIfElse(condition: Boolean, ifTrue: () -> R, ifFalse: () -> R, unrelated: () -> Unit): R? {
    contract {
        condition holdsIn ifTrue
        !condition holdsIn ifFalse
        callsInPlace(ifTrue)
        callsInPlace(ifFalse)
        callsInPlace(unrelated)
    }
    unrelated()
    return if (condition) ifTrue() else ifFalse()
}
