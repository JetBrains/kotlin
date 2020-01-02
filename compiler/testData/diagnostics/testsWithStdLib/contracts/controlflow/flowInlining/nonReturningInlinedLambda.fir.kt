// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun myRun(block: () -> Unit): Unit {
    contract {
        <!INAPPLICABLE_CANDIDATE!>callsInPlace<!>(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun getBool(): Boolean = false

fun withLabeledReturn() {
    val y: Int

    val x = myRun outer@ {
        myRun { return@outer Unit }
        y = 42
    }

    println(y)
    println(x)
}

fun withLabeledReturn2(y: Int) {
    myRun outer@ {
        myRun { return@outer Unit }
        println(y)
    }
    println(y)
}