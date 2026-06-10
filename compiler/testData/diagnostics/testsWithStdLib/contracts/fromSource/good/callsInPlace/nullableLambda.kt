// RUN_PIPELINE_TILL: BACKEND

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun runUnknown(block: (() -> Unit)?) {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    block?.invoke()
}

@OptIn(ExperimentalContracts::class)
fun runAtMostOnce(block: (() -> Unit)?) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    block?.invoke()
}

fun testUnknown(x : String?) {
    if (x != null) {
        runUnknown {
            println(x.length)
        }
    }
}

fun testAtMostOnce() {
    val x: Int
    runAtMostOnce {
        x = 1
    }
}

/* GENERATED_FIR_TAGS: assignment, classReference, contractCallsEffect, contracts, functionDeclaration, functionalType,
integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
