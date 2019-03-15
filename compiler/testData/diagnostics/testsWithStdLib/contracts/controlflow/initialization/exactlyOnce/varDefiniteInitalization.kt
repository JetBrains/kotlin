// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun definiteVarInitialization() {
    var x: Int
    myRun { x = 42 }
    x.inc()
}

fun definiteVarReassignment() {
    var x: Int
    myRun { x = 42 }
    x.inc()
    myRun { x = 43 }
    x.inc()
    x = 44
    x.inc()
}

fun nestedVarInitialization() {
    var x: Int
    myRun { myRun { myRun { x = 42 } } }
    x.inc()
    myRun { myRun { myRun { x = 42 } } }
}


fun notAnExpression() {
    var x: Int = 0
    myRun { if (true) x = 42 }
    x.inc()
}