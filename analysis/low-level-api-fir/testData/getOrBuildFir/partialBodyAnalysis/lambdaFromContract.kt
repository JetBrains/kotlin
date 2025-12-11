@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun runSomething(action: () -> Unit = <expr>{ }</expr>) {
    contract <expr_1>{
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }</expr_1>

    Unit
}
