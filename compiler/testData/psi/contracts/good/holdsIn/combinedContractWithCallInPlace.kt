// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.InvocationKind
import kotlin.contracts.*

inline fun <R> holdsInExactlyOnce(condition: Boolean, block: () -> R) {
    contract {
        condition holdsIn block
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

inline fun <R> holdsInAtLeastOnce(condition: Boolean, block: () -> R) {
    contract {
        condition holdsIn block
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

inline fun <R> holdsInWithTwoCallInPlace(condition: Boolean, block: () -> R, block2: ()-> R) {
    contract {
        condition holdsIn block
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block2, InvocationKind.AT_LEAST_ONCE)
    }
    block()
    block2()
}
