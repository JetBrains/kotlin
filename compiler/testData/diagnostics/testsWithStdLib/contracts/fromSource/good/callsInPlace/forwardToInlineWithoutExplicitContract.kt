// RUN_PIPELINE_TILL: BACKEND

import kotlin.contracts.*

inline fun inlineFun(init: (Int) -> Any) {
    init(0)
}

@OptIn(ExperimentalContracts::class)
fun exampleFun(size: Int, init: (Int) -> Any) {
    contract {
        callsInPlace(init, InvocationKind.UNKNOWN)
    }
    inlineFun(init)
}

/* GENERATED_FIR_TAGS: classReference, contractCallsEffect, contracts, functionDeclaration, functionalType, inline,
integerLiteral, lambdaLiteral */
